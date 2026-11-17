package com.ticketstream.order.domain.order;

import com.ticketstream.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse.Detail>> getOrder(@PathVariable Long id) {
        OrderResponse.Detail detail = orderService.getOrderDetail(id);
        return ResponseEntity.ok(ApiResponse.ok("주문 조회 성공", detail));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse.Detail>>> getOrdersByUser(@RequestParam Long userId) {
        List<OrderResponse.Detail> list = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("사용자 주문 목록 조회 성공", list));
    }
}
