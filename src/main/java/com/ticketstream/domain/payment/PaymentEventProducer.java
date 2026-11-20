package com.ticketstream.domain.payment;

import com.ticketstream.global.event.PaymentCompletedEvent;
import com.ticketstream.global.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";
    private static final String TOPIC_PAYMENT_FAILED = "payment.failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /* 성공 이벤트 */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        String key = String.valueOf(event.orderId());
        log.info("[PaymentEventProducer - publishPaymentCompleted] :: 성공 -> Topic: {}, OrderId: {}", TOPIC_PAYMENT_COMPLETED, key);

        kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED, key, event);
    }

    /* 실패 이벤트 */
    public void publicPaymentFailed(PaymentFailedEvent event) {
        String key = String.valueOf(event.orderId());
        log.info("[PaymentEventProducer - publicPaymentFailed] :: 실패 -> Topic: {}, OrderId: {}, 사유: {}", TOPIC_PAYMENT_FAILED, key, event.reason());

        kafkaTemplate.send(TOPIC_PAYMENT_FAILED, key, event);
    }

}
