package com.ticketstream.domain.order;

import com.ticketstream.global.enums.OrderStatus;
import com.ticketstream.global.event.SeatReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createPendingOrder(SeatReservedEvent event) {
        log.info("[OrderService - createPendingOrder] :: {}", event.toString());

        Order order = Order.builder()
                .userId(event.userId())
                .concertId(event.concertId())
                .seatId(event.seatId())
                .amount(event.price())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("[OrderService - createPendingOrder] :: save 완료 -> {}", savedOrder.getId());

        return savedOrder;
    }

}
