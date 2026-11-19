package com.ticketstream.domain.concert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertRepository concertRepository;

    public List<ConcertResponse.Info> getAllConcerts() {
        return concertRepository.findAll().stream()
                .map(ConcertResponse.Info::from)
                .toList();
    }

    public ConcertResponse.Info getConcert(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 ID입니다: " + id));
        return ConcertResponse.Info.from(concert);
    }
}
