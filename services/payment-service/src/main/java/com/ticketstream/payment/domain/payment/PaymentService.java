package com.ticketstream.payment.domain.payment;

import com.ticketstream.common.enums.PaymentStatus;
import com.ticketstream.common.event.PaymentCompletedEvent;
import com.ticketstream.common.event.PaymentFailedEvent;
import com.ticketstream.payment.kafka.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public PaymentResponse.Detail processPayment(PaymentRequest.Process request) {
        log.info("결제 승인 요청: orderId={}, seatId={}, amount={}", request.getOrderId(), request.getSeatId(), request.getAmount());

        try {
            // 결제 지연 모의 (실환경 외부 PG 호출 흉내: 50ms)
            Thread.sleep(50);
        } catch (InterruptedException ignored) {}

        // 테스트 편의상 금액이 음수인 경우 결제 실패 처리
        if (request.getAmount() <= 0) {
            paymentEventProducer.publishPaymentFailed(PaymentFailedEvent.builder()
                    .orderId(request.getOrderId())
                    .seatId(request.getSeatId())
                    .reason("유효하지 않은 결제 금액")
                    .failedAt(LocalDateTime.now())
                    .build());
            throw new IllegalArgumentException("유효하지 않은 결제 금액입니다: " + request.getAmount());
        }

        String pgTid = "PG_" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .seatId(request.getSeatId())
                .amount(request.getAmount())
                .pgTid(pgTid)
                .status(PaymentStatus.SUCCESS)
                .approvedAt(now)
                .build();

        payment = paymentRepository.save(payment);
        log.info("결제 승인 완료: paymentId={}, pgTid={}", payment.getId(), pgTid);

        // Kafka: payment.completed 발행 (order-service, seat-service가 구독)
        paymentEventProducer.publishPaymentCompleted(PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .seatId(payment.getSeatId())
                .amount(payment.getAmount())
                .completedAt(now)
                .build());

        return PaymentResponse.Detail.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .seatId(payment.getSeatId())
                .amount(payment.getAmount())
                .pgTid(payment.getPgTid())
                .status(payment.getStatus())
                .approvedAt(payment.getApprovedAt())
                .message("결제가 정상적으로 승인되었습니다.")
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentResponse.Detail getPaymentByOrderId(Long orderId) {
        Payment p = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문의 결제 내역이 없습니다: " + orderId));
        return PaymentResponse.Detail.builder()
                .paymentId(p.getId())
                .orderId(p.getOrderId())
                .seatId(p.getSeatId())
                .amount(p.getAmount())
                .pgTid(p.getPgTid())
                .status(p.getStatus())
                .approvedAt(p.getApprovedAt())
                .message("결제 조회 성공")
                .build();
    }
}
