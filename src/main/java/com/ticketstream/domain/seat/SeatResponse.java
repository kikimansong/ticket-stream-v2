package com.ticketstream.domain.seat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketstream.global.enums.SeatStatus;

import java.time.LocalDateTime;

public class SeatResponse {

    // 1. 좌석 목록 및 단건 조회용
    public record Info(
            Long id,
            Long concertId,
            String seatNumber,
            SeatStatus status,
            Long price,
            Long version
    ) {
        public static Info from(Seat seat) {
            return new Info(
                    seat.getId(),
                    seat.getConcertId(),
                    seat.getSeatNumber(),
                    seat.getStatus(),
                    seat.getPrice(),
                    seat.getVersion()
            );
        }
    }

    // 2. 좌석 선점 결과용
    public record ReserveResult(
            Long seatId,
            Long concertId,
            Long userId,
            String seatNumber,
            SeatStatus status,
            Long price,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") // ISO 포맷 보장
            LocalDateTime reservedAt,
            String message
    ) {
        // 기본 안내 메시지 상수
        private static final String DEFAULT_MESSAGE = "좌석이 선점되었습니다. 5분 내 결제를 완료해주세요.";

        public static ReserveResult of(Seat seat, Long userId) {
            return new ReserveResult(
                    seat.getId(),
                    seat.getConcertId(),
                    userId,
                    seat.getSeatNumber(),
                    seat.getStatus(),
                    seat.getPrice(),
                    LocalDateTime.now(),    // 선점 완료 시점
                    DEFAULT_MESSAGE         // 안내 메시지
            );
        }
    }

}
