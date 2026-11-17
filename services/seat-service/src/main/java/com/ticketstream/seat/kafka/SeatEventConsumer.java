package com.ticketstream.seat.kafka;

import com.ticketstream.common.event.PaymentCompletedEvent;
import com.ticketstream.common.event.PaymentFailedEvent;
import com.ticketstream.seat.domain.seat.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatEventConsumer {

    private final SeatService seatService;

    @KafkaListener(topics = "payment.completed", groupId = "seat-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("[Kafka] payment.completed 이벤트 수신: seatId={}, orderId={}", event.getSeatId(), event.getOrderId());
        seatService.completeSeatSale(event.getSeatId());
    }

    @KafkaListener(topics = "payment.failed", groupId = "seat-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("[Kafka] payment.failed 이벤트 수신 (보상 트랜잭션): seatId={}, reason={}", event.getSeatId(), event.getReason());
        seatService.releaseSeat(event.getSeatId());
    }
}
