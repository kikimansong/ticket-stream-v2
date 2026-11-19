package com.ticketstream.domain.seat;

import com.ticketstream.global.event.SeatReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatEventProducer {

    private static final String TOPIC_SEAT_RESERVED = "seat.reserved";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSeatReserved(SeatReservedEvent event) {
        String key = String.valueOf(event.seatId());

        log.info("[SeatEventProducer - publishSeatReserved] :: Event: {}", event);

        kafkaTemplate.send(TOPIC_SEAT_RESERVED, key, event)
                .whenComplete((result,exception) -> {
                    if (exception == null) {
                        log.info("[SeatEventProducer - publishSeatReserved] :: 발행 성공 -> offset: {}", result.getRecordMetadata().offset());
                    } else {
                        log.info("[SeatEventProducer - publishSeatReserved] :: 발행 실패 -> error: {}", exception.getMessage());
                    }
                });
    }

}
