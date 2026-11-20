package com.ticketstream.domain.payment;

import com.ticketstream.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse.Detail>> processPayment(@RequestBody PaymentRequest.Process request) {
        PaymentResponse.Detail response = paymentService.processPayment(request);

        return ResponseEntity.ok(ApiResponse.ok("결제 성공", response));
    }

}
