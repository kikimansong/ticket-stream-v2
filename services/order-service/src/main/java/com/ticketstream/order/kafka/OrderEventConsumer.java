package com.ticketstream.order.kafka;

import com.ticketstream.common.event.PaymentCompletedEvent;
import com.ticketstream.common.event.PaymentFailedEvent;
import com.ticketstream.common.event.SeatReservedEvent;
import com.ticketstream.order.domain.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "seat.reserved", groupId = "order-service-group")
    public void handleSeatReserved(SeatReservedEvent event) {
        log.info("[Kafka] seat.reserved 이벤트 수신 -> 가주문 생성 시작: seatId={}, userId={}", event.getSeatId(), event.getUserId());
        orderService.createPendingOrder(event.getUserId(), event.getConcertId(), event.getSeatId(), event.getPrice());
    }

    @KafkaListener(topics = "payment.completed", groupId = "order-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("[Kafka] payment.completed 이벤트 수신 -> 주문 완료 처리: orderId={}", event.getOrderId());
        orderService.completeOrder(event.getOrderId());
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("[Kafka] payment.failed 이벤트 수신 -> 주문 취소 처리: orderId={}, reason={}", event.getOrderId(), event.getReason());
        orderService.cancelOrder(event.getOrderId(), event.getReason());
    }
}
