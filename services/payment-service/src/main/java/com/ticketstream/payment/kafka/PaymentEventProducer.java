package com.ticketstream.payment.kafka;

import com.ticketstream.common.event.PaymentCompletedEvent;
import com.ticketstream.common.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";
    public static final String TOPIC_PAYMENT_FAILED = "payment.failed";

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[Kafka] payment.completed 이벤트 발행: paymentId={}, orderId={}", event.getPaymentId(), event.getOrderId());
        kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED, String.valueOf(event.getOrderId()), event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.warn("[Kafka] payment.failed 이벤트 발행: orderId={}, reason={}", event.getOrderId(), event.getReason());
        kafkaTemplate.send(TOPIC_PAYMENT_FAILED, String.valueOf(event.getOrderId()), event);
    }
}
