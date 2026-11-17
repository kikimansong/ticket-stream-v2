package com.ticketstream.waiting.domain.queue;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class QueueRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Enter {
        @NotNull(message = "사용자 ID는 필수입니다.")
        private Long userId;

        @NotNull(message = "공연 ID는 필수입니다.")
        private Long concertId;
    }
}
