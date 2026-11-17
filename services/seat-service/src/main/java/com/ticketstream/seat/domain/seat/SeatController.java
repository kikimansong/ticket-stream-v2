package com.ticketstream.seat.domain.seat;

import com.ticketstream.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SeatResponse.List>>> getSeats(@RequestParam Long concertId) {
        List<SeatResponse.List> list = seatService.getSeatsByConcert(concertId);
        return ResponseEntity.ok(ApiResponse.ok("좌석 목록 조회 성공", list));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<SeatResponse.ReserveResult>> reserveSeat(
            @Valid @RequestBody SeatRequest.Reserve request) {
        SeatResponse.ReserveResult result = seatService.reserveSeat(request);
        return ResponseEntity.ok(ApiResponse.ok("좌석 임시 선점 성공", result));
    }

    @PostMapping("/reset/{concertId}")
    public ResponseEntity<ApiResponse<Void>> resetSeats(@PathVariable Long concertId) {
        seatService.resetSeats(concertId);
        return ResponseEntity.ok(ApiResponse.ok("좌석 초기화 완료", null));
    }
}
