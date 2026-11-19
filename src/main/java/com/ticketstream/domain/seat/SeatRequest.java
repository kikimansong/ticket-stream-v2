package com.ticketstream.domain.seat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class SeatRequest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Reserve {
        private Long userId;
        private Long concertId;
        private Long seatId;
    }
}
