package com.ticketstream.domain.seat;

import com.ticketstream.global.enums.SeatStatus;
import com.ticketstream.global.event.SeatReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatEventProducer seatEventProducer;

    public List<SeatResponse.Info> getSeatsByConcert(Long concertId) {
        return seatRepository.findByConcertIdOrderByIdAsc(concertId).stream()
                .map(SeatResponse.Info::from)
                .toList();
    }

    @Transactional
    public SeatResponse.ReserveResult reserveSeat(SeatRequest.Reserve request) {
        Seat seat = seatRepository.findByIdAndConcertId(request.getSeatId(), request.getConcertId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다: " + request.getSeatId()));

        if (seat.getStatus() == SeatStatus.AVAILABLE) {
            seat.setStatus(SeatStatus.RESERVED);    // seat.reserve();
        } else {
            throw new IllegalStateException("이미 선점되었거나 판매 완료 된 좌석입니다: " + request.getSeatId());
        }

        SeatReservedEvent event = SeatReservedEvent.of(
                request.getUserId(),
                seat.getConcertId(),
                seat.getId(),
                seat.getSeatNumber(),
                seat.getPrice()
        );

        seatEventProducer.publishSeatReserved(event);

        return SeatResponse.ReserveResult.of(seat, request.getUserId());
    }

    @Transactional
    public void resetSeats(Long concertId) {
        log.info("[SeatService] 공연 ID {} 좌석 전체 초기화 (AVAILABLE)", concertId);
        seatRepository.resetSeatsByConcertId(concertId);
    }
}
