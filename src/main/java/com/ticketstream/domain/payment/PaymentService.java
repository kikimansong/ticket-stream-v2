package com.ticketstream.domain.payment;

import com.ticketstream.domain.order.Order;
import com.ticketstream.domain.order.OrderRepository;
import com.ticketstream.domain.seat.Seat;
import com.ticketstream.domain.seat.SeatRepository;
import com.ticketstream.global.enums.OrderStatus;
import com.ticketstream.global.enums.PaymentStatus;
import com.ticketstream.global.enums.SeatStatus;
import com.ticketstream.global.event.PaymentCompletedEvent;
import com.ticketstream.global.event.PaymentFailedEvent;
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
    private final SeatRepository seatRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResponse.Detail processPayment(PaymentRequest.Process request) {
        log.info("[PaymentService - processPayment] :: orderId: {}, seatId: {}, amount: {}",
                request.getOrderId(), request.getSeatId(), request.getAmount());

        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다: " + request.getSeatId()));

        if (seat.getStatus() == SeatStatus.AVAILABLE) {
            throw new IllegalStateException("선점되지 않은 좌석입니다");
        }

        if (seat.getStatus() == SeatStatus.SOLD) {
            throw new IllegalStateException("이미 판매 완료된 좌석입니다.");
        }

        Order order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId()).orElse(null);
        }
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            order = orderRepository.findFirstBySeatIdAndStatusOrderByCreatedAtDesc(request.getSeatId(), OrderStatus.PENDING)
                    .orElseThrow(() -> new IllegalArgumentException("결제 대기(PENDING) 상태의 주문이 존재하지 않습니다: seatId=" + request.getSeatId()));
        }

        Long actualOrderId = order.getId();

        try {
            String pgTid = "PG_" + UUID.randomUUID().toString().substring(0, 8);

            Payment payment = Payment.builder()
                    .orderId(actualOrderId)
                    .seatId(request.getSeatId())
                    .amount(request.getAmount())
                    .pgTid(pgTid)
                    .status(PaymentStatus.SUCCESS)
                    .approvedAt(LocalDateTime.now())
                    .build();

            Payment savedPayment = paymentRepository.save(payment);

            paymentEventProducer.publishPaymentCompleted(PaymentCompletedEvent.of(
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getSeatId(),
                    savedPayment.getAmount(),
                    savedPayment.getPgTid()
            ));

            return PaymentResponse.Detail.from(savedPayment);

        } catch (Exception e) {
            log.error("[PaymentService - processPayment] :: failed -> {}", e.getMessage());

            paymentEventProducer.publicPaymentFailed(PaymentFailedEvent.of(
                    request.getOrderId(),
                    request.getSeatId(),
                    e.getMessage()
            ));

            throw new IllegalStateException("결제 실패: " + e.getMessage());
        }
    }

}
