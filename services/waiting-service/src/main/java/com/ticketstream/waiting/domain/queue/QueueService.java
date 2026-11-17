package com.ticketstream.waiting.domain.queue;

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

    private static final String WAITING_KEY_PREFIX = "queue:waiting:";
    private static final String ALLOWED_KEY_PREFIX = "queue:allowed:";
    private static final String TOKEN_META_PREFIX = "queue:token:";

    public QueueResponse.Status enterQueue(Long concertId, Long userId) {
        String token = UUID.randomUUID().toString();
        String waitingKey = WAITING_KEY_PREFIX + concertId;
        String tokenMetaKey = TOKEN_META_PREFIX + token;

        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(waitingKey, token, now);
        redisTemplate.opsForValue().set(tokenMetaKey, userId + ":" + concertId, Duration.ofMinutes(30));

        Long rank = redisTemplate.opsForZSet().rank(waitingKey, token);
        Long total = redisTemplate.opsForZSet().zCard(waitingKey);

        log.info("대기열 진입 완료: concertId={}, userId={}, token={}, rank={}", concertId, userId, token, rank);

        return QueueResponse.Status.builder()
                .token(token)
                .userId(userId)
                .concertId(concertId)
                .rank(rank != null ? rank + 1 : 1)
                .totalWaiting(total != null ? total : 1)
                .allowed(false)
                .message("대기열에 등록되었습니다. 잠시만 기다려주세요.")
                .build();
    }

    public QueueResponse.Status getQueueStatus(Long concertId, String token) {
        String allowedKey = ALLOWED_KEY_PREFIX + concertId;
        Boolean isAllowed = redisTemplate.opsForSet().isMember(allowedKey, token);

        if (Boolean.TRUE.equals(isAllowed)) {
            return QueueResponse.Status.builder()
                    .token(token)
                    .concertId(concertId)
                    .rank(0L)
                    .totalWaiting(0L)
                    .allowed(true)
                    .message("입장 가능한 상태입니다.")
                    .build();
        }

        String waitingKey = WAITING_KEY_PREFIX + concertId;
        Long rank = redisTemplate.opsForZSet().rank(waitingKey, token);
        Long total = redisTemplate.opsForZSet().zCard(waitingKey);

        if (rank == null) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 대기 토큰입니다.");
        }

        return QueueResponse.Status.builder()
                .token(token)
                .concertId(concertId)
                .rank(rank + 1)
                .totalWaiting(total != null ? total : 0)
                .allowed(false)
                .message("현재 대기 중입니다.")
                .build();
    }

    public void allowEntries(Long concertId, long count) {
        String waitingKey = WAITING_KEY_PREFIX + concertId;
        String allowedKey = ALLOWED_KEY_PREFIX + concertId;

        Set<String> tokens = redisTemplate.opsForZSet().range(waitingKey, 0, count - 1);
        if (tokens != null && !tokens.isEmpty()) {
            for (String token : tokens) {
                redisTemplate.opsForZSet().remove(waitingKey, token);
                redisTemplate.opsForSet().add(allowedKey, token);
            }
            redisTemplate.expire(allowedKey, Duration.ofMinutes(15));
            log.info("대기열 입장 허용 완료: concertId={}, 허용인원={}", concertId, tokens.size());
        }
    }
}
