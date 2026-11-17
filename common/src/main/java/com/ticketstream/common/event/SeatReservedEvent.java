package com.ticketstream.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservedEvent {
    private Long seatId;
    private Long concertId;
    private Long userId;
    private Long price;
    private LocalDateTime reservedAt;
}
