package com.ticketstream.payment.domain.payment;

import com.ticketstream.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class PaymentResponse {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private Long paymentId;
        private Long orderId;
        private Long seatId;
        private Long amount;
        private String pgTid;
        private PaymentStatus status;
        private LocalDateTime approvedAt;
        private String message;
    }
}
