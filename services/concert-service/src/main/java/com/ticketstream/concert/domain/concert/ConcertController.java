package com.ticketstream.concert.domain.concert;

import com.ticketstream.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConcertResponse.List>>> getAllConcerts() {
        List<ConcertResponse.List> list = concertService.getAllConcerts();
        return ResponseEntity.ok(ApiResponse.ok("공연 목록 조회 성공", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConcertResponse.Detail>> getConcert(@PathVariable Long id) {
        ConcertResponse.Detail detail = concertService.getConcert(id);
        return ResponseEntity.ok(ApiResponse.ok("공연 상세 조회 성공", detail));
    }
}
