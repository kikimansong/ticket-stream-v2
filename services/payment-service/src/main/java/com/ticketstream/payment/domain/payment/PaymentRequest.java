package com.ticketstream.payment.domain.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PaymentRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Process {
        @NotNull(message = "주문 ID는 필수입니다.")
        private Long orderId;

        @NotNull(message = "좌석 ID는 필수입니다.")
        private Long seatId;

        @NotNull(message = "결제 금액은 필수입니다.")
        private Long amount;

        private String paymentMethod; // CARD, KAKAO_PAY, TOSS_PAY 등
    }
}
