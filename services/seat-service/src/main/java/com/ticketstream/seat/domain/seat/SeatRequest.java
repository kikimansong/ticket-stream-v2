package com.ticketstream.seat.domain.seat;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class SeatRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Reserve {
        @NotNull(message = "사용자 ID는 필수입니다.")
        private Long userId;

        @NotNull(message = "공연 ID는 필수입니다.")
        private Long concertId;

        @NotNull(message = "좌석 ID는 필수입니다.")
        private Long seatId;
    }
}
