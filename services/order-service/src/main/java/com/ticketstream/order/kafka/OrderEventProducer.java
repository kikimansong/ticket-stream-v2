package com.ticketstream.order.kafka;

import com.ticketstream.common.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_ORDER_CREATED = "order.created";

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("[Kafka] order.created 이벤트 발행: orderId={}, userId={}", event.getOrderId(), event.getUserId());
        kafkaTemplate.send(TOPIC_ORDER_CREATED, String.valueOf(event.getOrderId()), event);
    }
}
