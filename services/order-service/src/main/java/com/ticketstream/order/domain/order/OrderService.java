package com.ticketstream.order.domain.order;

import com.ticketstream.common.enums.OrderStatus;
import com.ticketstream.common.event.OrderCreatedEvent;
import com.ticketstream.order.kafka.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse.Detail createPendingOrder(Long userId, Long concertId, Long seatId, Long amount) {
        Order order = Order.builder()
                .userId(userId)
                .concertId(concertId)
                .seatId(seatId)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);
        log.info("가주문(PENDING) 생성 성공: orderId={}, seatId={}", order.getId(), seatId);

        // order.created 이벤트 발행
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .concertId(order.getConcertId())
                .seatId(order.getSeatId())
                .amount(order.getAmount())
                .createdAt(order.getCreatedAt())
                .build();

        orderEventProducer.publishOrderCreated(event);

        return toDetailResponse(order, "가주문이 성공적으로 생성되었습니다. 결제를 진행해주세요.");
    }

    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));
        order.complete();
        log.info("주문 최종 완료 처리: orderId={}, status=COMPLETED", orderId);
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));
        order.cancel();
        log.warn("주문 취소 처리 완료: orderId={}, reason={}", orderId, reason);
    }

    @Transactional(readOnly = true)
    public OrderResponse.Detail getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));
        return toDetailResponse(order, "주문 상세 조회 성공");
    }

    @Transactional(readOnly = true)
    public List<OrderResponse.Detail> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(o -> toDetailResponse(o, null))
                .toList();
    }

    private OrderResponse.Detail toDetailResponse(Order order, String message) {
        return OrderResponse.Detail.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .concertId(order.getConcertId())
                .seatId(order.getSeatId())
                .amount(order.getAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .message(message)
                .build();
    }
}
