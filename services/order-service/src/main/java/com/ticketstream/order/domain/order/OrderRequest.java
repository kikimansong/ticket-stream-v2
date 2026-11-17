package com.ticketstream.order.domain.order;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class OrderRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Create {
        @NotNull(message = "사용자 ID는 필수입니다.")
        private Long userId;

        @NotNull(message = "공연 ID는 필수입니다.")
        private Long concertId;

        @NotNull(message = "좌석 ID는 필수입니다.")
        private Long seatId;

        @NotNull(message = "결제 금액은 필수입니다.")
        private Long amount;
    }
}
