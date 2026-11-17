package com.ticketstream.waiting.domain.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class QueueResponse {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        private String token;
        private Long userId;
        private Long concertId;
        private Long rank;
        private Long totalWaiting;
        private boolean allowed;
        private String message;
    }
}
