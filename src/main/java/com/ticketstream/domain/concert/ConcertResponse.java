package com.ticketstream.domain.concert;

import java.time.LocalDateTime;

public class ConcertResponse {

    public record Info(
            Long id,
            String title,
            String venue,
            LocalDateTime concertDate,
            int totalSeats
    ) {
        public static Info from(Concert concert) {
            return new Info(
                    concert.getId(),
                    concert.getTitle(),
                    concert.getVenue(),
                    concert.getConcertDate(),
                    concert.getTotalSeats()
            );
        }
    }
}
