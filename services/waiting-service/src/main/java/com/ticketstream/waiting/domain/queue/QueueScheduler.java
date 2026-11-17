package com.ticketstream.waiting.domain.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueScheduler {

    private final QueueService queueService;
    private final StringRedisTemplate redisTemplate;

    private static final String WAITING_KEY_PREFIX = "queue:waiting:";

    // 1초마다 대기열 상위 20명씩 입장 허용
    @Scheduled(fixedRate = 1000)
    public void processQueue() {
        Set<String> keys = redisTemplate.keys(WAITING_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            try {
                Long concertId = Long.parseLong(key.replace(WAITING_KEY_PREFIX, ""));
                queueService.allowEntries(concertId, 20);
            } catch (Exception e) {
                log.warn("대기열 자동 진입 처리 중 오류: key={}, msg={}", key, e.getMessage());
            }
        }
    }
}
