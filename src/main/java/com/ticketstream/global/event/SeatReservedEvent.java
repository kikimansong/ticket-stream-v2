package com.ticketstream.global.event;

import java.time.LocalDateTime;

public record SeatReservedEvent(
        Long userId,
        Long concertId,
        Long seatId,
        String seatNumber,
        Long price,
        LocalDateTime reservedAt
) {
    public static SeatReservedEvent of(Long userId, Long concertId, Long seatId, String seatNumber, Long price) {
        return new SeatReservedEvent(
                userId,
                concertId,
                seatId,
                seatNumber,
                price,
                LocalDateTime.now()
        );
    }
}