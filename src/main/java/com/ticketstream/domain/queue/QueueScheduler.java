package com.ticketstream.domain.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueScheduler {

    private final QueueService queueService;
    private static final long ALLOW_COUNT_PER_SECOND = 50; // 초당 50명씩 입장 허가

    // 1초(1000ms)마다 대기열 상위 인원 입장 허가
    @Scheduled(fixedDelay = 1000)
    public void scheduleQueueEntries() {
        // 등록된 콘서트 대상 (1번, 2번 공연)
        queueService.allowEntries(1L, ALLOW_COUNT_PER_SECOND);
        queueService.allowEntries(2L, ALLOW_COUNT_PER_SECOND);
    }
}
