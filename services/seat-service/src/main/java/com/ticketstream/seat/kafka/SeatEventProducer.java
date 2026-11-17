package com.ticketstream.seat.kafka;

import com.ticketstream.common.event.SeatReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_SEAT_RESERVED = "seat.reserved";

    public void publishSeatReserved(SeatReservedEvent event) {
        log.info("[Kafka] seat.reserved 이벤트 발행: seatId={}, userId={}", event.getSeatId(), event.getUserId());
        kafkaTemplate.send(TOPIC_SEAT_RESERVED, String.valueOf(event.getSeatId()), event);
    }
}
