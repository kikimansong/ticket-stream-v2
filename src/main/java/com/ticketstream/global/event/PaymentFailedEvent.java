package com.ticketstream.global.event;

import java.time.LocalDateTime;

public record PaymentFailedEvent(
        Long orderId,
        Long seatId,
        String reason,
        LocalDateTime failedAt
) {
    public static PaymentFailedEvent of(Long orderId, Long seatId, String reason) {
        return new PaymentFailedEvent(orderId, seatId, reason, LocalDateTime.now());
    }
}
