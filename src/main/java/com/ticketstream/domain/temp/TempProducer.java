package com.ticketstream.domain.temp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TempProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "temp.created";

    public void sendTempCreated(String key, Object Message) {
        kafkaTemplate.send(TOPIC, key, Message);
    }

}
