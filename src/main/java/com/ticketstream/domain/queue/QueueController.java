package com.ticketstream.domain.queue;

import com.ticketstream.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    // 1. 대기열 진입 및 순번표(Token) 발급
    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<QueueResponse.TokenInfo>> enterQueue(
            @RequestBody QueueRequest.Enter request) {
        QueueResponse.TokenInfo response = queueService.enterQueue(request);
        return ResponseEntity.ok(ApiResponse.ok("대기열 진입 성공", response));
    }

    // 2. 대기열 순번 폴링 상태 조회 (헤더 또는 쿼리 파라미터 지원)
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<QueueResponse.TokenInfo>> getQueueStatus(
            @RequestHeader(value = "X-Queue-Token", required = false) String headerToken,
            @RequestParam(value = "token", required = false) String paramToken,
            @RequestParam Long concertId) {

        String token = (headerToken != null && !headerToken.isBlank()) ? headerToken : paramToken;
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("대기열 토큰(X-Queue-Token 또는 token)이 제공되지 않았습니다.");
        }

        QueueResponse.TokenInfo response = queueService.getQueueStatus(token, concertId);
        return ResponseEntity.ok(ApiResponse.ok("대기열 상태 조회 성공", response));
    }
}
