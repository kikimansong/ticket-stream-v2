package com.ticketstream.seat.domain.seat;

import com.ticketstream.common.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SeatResponse {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class List {
        private Long id;
        private Long concertId;
        private String seatNumber;
        private SeatStatus status;
        private Long price;
        private Long version;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReserveResult {
        private Long seatId;
        private Long concertId;
        private Long userId;
        private String seatNumber;
        private SeatStatus status;
        private Long price;
        private LocalDateTime reservedAt;
        private String message;
    }
}
