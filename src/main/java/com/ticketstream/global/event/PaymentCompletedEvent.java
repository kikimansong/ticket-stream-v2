package com.ticketstream.global.event;

import java.time.LocalDateTime;

public record PaymentCompletedEvent(
        Long paymentId,
        Long orderId,
        Long seatId,
        Long amount,
        String pgTid,
        LocalDateTime approvedAt
) {
    public static PaymentCompletedEvent of(Long paymentId, Long orderId, Long seatId, Long amount, String pgTid) {
        return new PaymentCompletedEvent(paymentId, orderId, seatId, amount, pgTid, LocalDateTime.now());
    }
}
