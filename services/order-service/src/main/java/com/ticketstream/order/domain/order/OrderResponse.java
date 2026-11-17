package com.ticketstream.order.domain.order;

import com.ticketstream.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class OrderResponse {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private Long orderId;
        private Long userId;
        private Long concertId;
        private Long seatId;
        private Long amount;
        private OrderStatus status;
        private LocalDateTime createdAt;
        private String message;
    }
}
