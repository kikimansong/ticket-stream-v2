package com.ticketstream.domain.payment;

import com.ticketstream.global.enums.PaymentStatus;

import java.time.LocalDateTime;

public class PaymentResponse {

    // 결제 승인 상세 응답용
    public record Detail(
            Long paymentId,
            Long orderId,
            Long seatId,
            Long amount,
            PaymentStatus status,
            String pgTid,
            LocalDateTime approvedAt
    ) {
        public static Detail from(Payment payment) {
            return new Detail(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getSeatId(),
                    payment.getAmount(),
                    payment.getStatus(),
                    payment.getPgTid(),
                    payment.getApprovedAt()
            );
        }
    }
}
