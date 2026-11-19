package com.ticketstream.domain.seat;

import com.ticketstream.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SeatResponse.Info>>> getSeats(@RequestParam Long concertId) {
        List<SeatResponse.Info> response = seatService.getSeatsByConcert(concertId);
        return ResponseEntity.ok(ApiResponse.ok("좌석 목록 조회 성공", response));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<SeatResponse.ReserveResult>> reserveSeat(@RequestBody SeatRequest.Reserve request) {
        SeatResponse.ReserveResult response = seatService.reserveSeat(request);

        return ResponseEntity.ok(ApiResponse.ok("좌석 선점 성공", response));
    }

}
