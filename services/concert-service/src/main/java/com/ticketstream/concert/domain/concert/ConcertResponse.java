package com.ticketstream.concert.domain.concert;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ConcertResponse {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class List {
        private Long id;
        private String title;
        private String venue;
        private LocalDateTime concertDate;
        private int totalSeats;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private Long id;
        private String title;
        private String venue;
        private LocalDateTime concertDate;
        private int totalSeats;
    }
}
