package com.ticketstream.domain.temp;

import com.ticketstream.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/temp")
@RequiredArgsConstructor
public class TempController {

    private final TempService tempService;

    @PostMapping
    public ApiResponse<Integer> createTemp() {
        return ApiResponse.ok(tempService.createTemp());
    }

}
