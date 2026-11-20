package com.ticketstream.domain.seat;

import com.ticketstream.global.event.PaymentCompletedEvent;
import com.ticketstream.global.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatEventConsumer {

    private final SeatRepository seatRepository;

    /* 좌석 판매 확정 (SOLD) */
    @KafkaListener(topics = "payment.completed", groupId = "seat-group")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        seatRepository.findById(event.seatId()).ifPresent(seat -> {
            seat.completeSale();
        });
    }

    /* 좌석 판매 실패 (AVAILABLE 원복) */
    @KafkaListener(topics = "payment.failed", groupId = "seat-group")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        seatRepository.findById(event.seatId()).ifPresent(seat -> {
            seat.release();
        });
    }

}
