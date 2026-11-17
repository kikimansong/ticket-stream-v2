package com.ticketstream.concert.domain.concert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertRepository concertRepository;

    public List<ConcertResponse.List> getAllConcerts() {
        return concertRepository.findAll().stream()
                .map(c -> new ConcertResponse.List(
                        c.getId(),
                        c.getTitle(),
                        c.getVenue(),
                        c.getConcertDate(),
                        c.getTotalSeats()
                ))
                .toList();
    }

    public ConcertResponse.Detail getConcert(Long id) {
        Concert c = concertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 ID입니다: " + id));
        return new ConcertResponse.Detail(
                c.getId(),
                c.getTitle(),
                c.getVenue(),
                c.getConcertDate(),
                c.getTotalSeats()
        );
    }
}
