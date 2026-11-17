package com.ticketstream.waiting.domain.queue;

import com.ticketstream.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<QueueResponse.Status>> enterQueue(@Valid @RequestBody QueueRequest.Enter request) {
        QueueResponse.Status response = queueService.enterQueue(request.getConcertId(), request.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("대기열 등록 성공", response));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<QueueResponse.Status>> getStatus(
            @RequestParam Long concertId,
            @RequestHeader("X-Queue-Token") String token) {
        QueueResponse.Status response = queueService.getQueueStatus(concertId, token);
        return ResponseEntity.ok(ApiResponse.ok("대기열 상태 조회 성공", response));
    }
}
