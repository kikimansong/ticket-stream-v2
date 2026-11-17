package com.ticketstream.payment.domain.payment;

import com.ticketstream.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse.Detail>> processPayment(
            @Valid @RequestBody PaymentRequest.Process request) {
        PaymentResponse.Detail detail = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.ok("결제 성공", detail));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse.Detail>> getPayment(@PathVariable Long orderId) {
        PaymentResponse.Detail detail = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok("결제 상세 조회 성공", detail));
    }
}
