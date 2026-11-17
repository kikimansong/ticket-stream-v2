package com.ticketstream.seat.domain.seat;

import com.ticketstream.common.event.SeatReservedEvent;
import com.ticketstream.seat.kafka.SeatEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final SeatRepository seatRepository;
    private final RedissonClient redissonClient;
    private final SeatEventProducer seatEventProducer;

    @Transactional(readOnly = true)
    public List<SeatResponse.List> getSeatsByConcert(Long concertId) {
        return seatRepository.findByConcertIdOrderByIdAsc(concertId).stream()
                .map(s -> new SeatResponse.List(
                        s.getId(),
                        s.getConcertId(),
                        s.getSeatNumber(),
                        s.getStatus(),
                        s.getPrice(),
                        s.getVersion()
                ))
                .toList();
    }

    @Transactional
    public SeatResponse.ReserveResult reserveSeat(SeatRequest.Reserve request) {
        Long seatId = request.getSeatId();
        String lockKey = "lock:seat:" + seatId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired;
        try {
            // 대기 2초, 5분(300초) 동안 락 유지 (임시 선점 시간)
            acquired = lock.tryLock(2, 300, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("좌석 선점 락 획득 중 인터럽트가 발생했습니다.", e);
        }

        if (!acquired) {
            throw new IllegalStateException("현재 다른 사용자가 선점 중인 좌석입니다: " + seatId);
        }

        try {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다: " + seatId));

            // 비즈니스 검증 및 상태 변경 (AVAILABLE -> RESERVED)
            seat.reserve();

            // Kafka 이벤트 발행 (order-service 등이 구독)
            LocalDateTime now = LocalDateTime.now();
            SeatReservedEvent event = SeatReservedEvent.builder()
                    .seatId(seat.getId())
                    .concertId(seat.getConcertId())
                    .userId(request.getUserId())
                    .price(seat.getPrice())
                    .reservedAt(now)
                    .build();

            seatEventProducer.publishSeatReserved(event);

            log.info("좌석 임시 선점 성공: seatId={}, userId={}, seatNumber={}", seat.getId(), request.getUserId(), seat.getSeatNumber());

            return SeatResponse.ReserveResult.builder()
                    .seatId(seat.getId())
                    .concertId(seat.getConcertId())
                    .userId(request.getUserId())
                    .seatNumber(seat.getSeatNumber())
                    .status(seat.getStatus())
                    .price(seat.getPrice())
                    .reservedAt(now)
                    .message("좌석이 임시 선점되었습니다. 5분 내에 결제를 완료해주세요.")
                    .build();

        } catch (Exception e) {
            // 실패 시 락 즉시 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            throw e;
        }
    }

    @Transactional
    public void completeSeatSale(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다: " + seatId));
        seat.completeSale();

        // 판매 확정 후 분산 락 정리
        RLock lock = redissonClient.getLock("lock:seat:" + seatId);
        if (lock.isLocked()) {
            lock.forceUnlock();
        }
        log.info("좌석 판매 확정 완료: seatId={}, status=SOLD", seatId);
    }

    @Transactional
    public void releaseSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다: " + seatId));
        seat.release();

        // 선점 해제 시 락 해제
        RLock lock = redissonClient.getLock("lock:seat:" + seatId);
        if (lock.isLocked()) {
            lock.forceUnlock();
        }
        log.info("좌석 선점 롤백 완료: seatId={}, status=AVAILABLE", seatId);
    }

    @Transactional
    public void resetSeats(Long concertId) {
        log.info("공연 좌석 리셋 시작: concertId={}", concertId);
        seatRepository.resetSeatsByConcertId(concertId);
        log.info("공연 좌석 리셋 완료: concertId={}", concertId);
    }
}
