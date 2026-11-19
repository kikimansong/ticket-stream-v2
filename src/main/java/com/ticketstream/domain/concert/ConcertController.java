package com.ticketstream.domain.concert;

import com.ticketstream.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConcertResponse.Info>>> getAllConcerts() {
        List<ConcertResponse.Info> response = concertService.getAllConcerts();
        return ResponseEntity.ok(ApiResponse.ok("공연 목록 조회 성공", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConcertResponse.Info>> getConcert(@PathVariable Long id) {
        ConcertResponse.Info response = concertService.getConcert(id);
        return ResponseEntity.ok(ApiResponse.ok("공연 상세 조회 성공", response));
    }
}
