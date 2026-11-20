package com.ticketstream.domain.order;

import com.ticketstream.global.event.PaymentCompletedEvent;
import com.ticketstream.global.event.PaymentFailedEvent;
import com.ticketstream.global.event.SeatReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    /* 가주문 생성 (PENDING) */
    @KafkaListener(topics = "seat.reserved", groupId = "order-group")
    public void handleSeatReserved(SeatReservedEvent event) {
        log.info("[OrderEventConsumer - handleSeatReserved] :: {}", event);

        try {
            orderService.createPendingOrder(event);
        } catch (Exception e) {
            log.info("[OrderEventConsumer - handleSeatReserved] :: failed -> {}", e.getMessage());
        }
    }

    /* 주문 확정 (COMPLETED) */
    @KafkaListener(topics = "payment.completed", groupId = "order-group")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.complete();
        });
    }

    /* 결제 취소 (CANCELLED) */
    @KafkaListener(topics = "payment.failed", groupId = "order-group")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.cancel();
        });
    }

}
