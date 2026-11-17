package com.ticketstream.concert.domain.concert;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class ConcertRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Create {
        @NotBlank(message = "공연명은 필수입니다.")
        private String title;
        private String venue;
        private LocalDateTime concertDate;
        private int totalSeats;
    }
}
