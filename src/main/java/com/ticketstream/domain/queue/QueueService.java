package com.ticketstream.domain.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final StringRedisTemplate redisTemplate;

    private static final String WAITING_QUEUE_PREFIX = "queue:waiting:concert:";
    private static final String ACTIVE_TOKEN_PREFIX = "queue:active:";
    private static final String TOKEN_META_PREFIX = "queue:meta:";
    private static final Duration ACTIVE_TOKEN_TTL = Duration.ofMinutes(5);

    // 1. 대기열 진입 (토큰 발급 및 ZSet 등록)
    public QueueResponse.TokenInfo enterQueue(QueueRequest.Enter request) {
        String token = UUID.randomUUID().toString();
        String waitingKey = WAITING_QUEUE_PREFIX + request.getConcertId();
        double score = System.currentTimeMillis();

        // Redis ZSet에 등록 (score = 진입 타임스탬프)
        redisTemplate.opsForZSet().add(waitingKey, token, score);

        // 토큰 메타데이터(userId) 보관
        redisTemplate.opsForValue().set(TOKEN_META_PREFIX + token, String.valueOf(request.getUserId()), Duration.ofHours(1));

        // 내 현재 순번 및 전체 대기 인원 조회
        Long rank = redisTemplate.opsForZSet().rank(waitingKey, token);
        Long totalWaiting = redisTemplate.opsForZSet().size(waitingKey);

        long currentRank = (rank != null) ? rank + 1 : 1L;
        long total = (totalWaiting != null) ? totalWaiting : 1L;

        log.info("[QueueService] 대기열 진입 완료 -> userId: {}, concertId: {}, rank: {}/{}",
                request.getUserId(), request.getConcertId(), currentRank, total);

        return new QueueResponse.TokenInfo(
                token,
                request.getUserId(),
                request.getConcertId(),
                currentRank,
                total,
                false,
                "대기열에 진입했습니다."
        );
    }

    // 2. 대기열 순번 폴링 상태 조회
    public QueueResponse.TokenInfo getQueueStatus(String token, Long concertId) {
        String metaUserId = redisTemplate.opsForValue().get(TOKEN_META_PREFIX + token);
        Long userId = (metaUserId != null) ? Long.valueOf(metaUserId) : null;

        // 1) 이미 활성화(입장 허가)된 토큰인지 확인
        Boolean isActive = redisTemplate.hasKey(ACTIVE_TOKEN_PREFIX + token);
        if (Boolean.TRUE.equals(isActive)) {
            log.info("[QueueService] 활성 토큰 확인 -> 입장 허가: token={}", token);
            return new QueueResponse.TokenInfo(
                    token,
                    userId,
                    concertId,
                    0L,
                    0L,
                    true,
                    "입장이 허가되었습니다."
            );
        }

        // 2) 아직 대기열에 있는 경우 현재 순위 확인
        String waitingKey = WAITING_QUEUE_PREFIX + concertId;
        Long rank = redisTemplate.opsForZSet().rank(waitingKey, token);
        Long totalWaiting = redisTemplate.opsForZSet().size(waitingKey);

        if (rank == null) {
            log.warn("[QueueService] 대기열에 존재하지 않는 토큰: token={}", token);
            throw new IllegalArgumentException("대기열에 존재하지 않거나 이미 만료된 토큰입니다.");
        }

        long currentRank = rank + 1;
        long total = (totalWaiting != null) ? totalWaiting : 0L;

        return new QueueResponse.TokenInfo(
                token,
                userId,
                concertId,
                currentRank,
                total,
                false,
                "대기 중입니다. (내 순번: " + currentRank + "위)"
        );
    }

    // 3. 상위 N명 입장 허가 (스케줄러 주기적 호출)
    public void allowEntries(Long concertId, long count) {
        String waitingKey = WAITING_QUEUE_PREFIX + concertId;

        // 대기열 맨 앞 N명 조회
        Set<String> tokens = redisTemplate.opsForZSet().range(waitingKey, 0, count - 1);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (String token : tokens) {
            // 1) 대기열에서 제거
            redisTemplate.opsForZSet().remove(waitingKey, token);
            // 2) 활성 토큰 등록 (TTL 5분)
            redisTemplate.opsForValue().set(ACTIVE_TOKEN_PREFIX + token, "ACTIVE", ACTIVE_TOKEN_TTL);
        }

        log.info("[QueueService] 공연 ID {} 대기열 상위 {}명 입장 허가 완료!", concertId, tokens.size());
    }

    // 4. 활성 토큰 유효성 검증
    public void validateActiveToken(String token) {
        if (token == null || token.isBlank() || !Boolean.TRUE.equals(redisTemplate.hasKey(ACTIVE_TOKEN_PREFIX + token))) {
            throw new IllegalStateException("대기열을 통과한 유효한 토큰이 아닙니다. 먼저 대기열에 진입해주세요.");
        }
    }

}
