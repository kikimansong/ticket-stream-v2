package com.ticketstream.domain.temp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TempConsumer {

    @KafkaListener(topics = "temp.created", groupId = "temp-group")
    public void handleTempCreated(Object event) {
        log.info("[TempConsumer] - event :: {}", event);
    }

}
