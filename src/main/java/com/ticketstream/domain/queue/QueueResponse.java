package com.ticketstream.domain.queue;

public class QueueResponse {

    // 대기열 토큰 발급 및 상태 폴링 공통 응답용
    public record TokenInfo(
            String token,
            Long userId,
            Long concertId,
            Long rank,
            Long totalWaiting,
            boolean allowed,
            String message
    ) {}

    // 대기열 순번 폴링 상태 응답용
    public record Status(
            String token,
            Long userId,
            Long concertId,
            Long rank,
            Long totalWaiting,
            boolean allowed,
            String message
    ) {}
}
