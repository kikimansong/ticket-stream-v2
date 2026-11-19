package com.ticketstream.domain.order;

import com.ticketstream.global.enums.OrderStatus;

import java.time.LocalDateTime;

public class OrderResponse {

    // 1. 주문 상세 조회 및 생성 완료 응답용
    public record Detail(
            Long orderId,
            Long userId,
            Long concertId,
            Long seatId,
            Long amount,
            OrderStatus status,
            LocalDateTime createdAt
    ) {
        public static Detail from(Order order) {
            return new Detail(
                    order.getId(),
                    order.getUserId(),
                    order.getConcertId(),
                    order.getSeatId(),
                    order.getAmount(),
                    order.getStatus(),
                    order.getCreatedAt()
            );
        }
    }

    // 2. 주문 목록 조회용
    public record List(
            Long orderId,
            Long userId,
            Long concertId,
            Long seatId,
            Long amount,
            OrderStatus status,
            LocalDateTime createdAt
    ) {
        public static List from(Order order) {
            return new List(
                    order.getId(),
                    order.getUserId(),
                    order.getConcertId(),
                    order.getSeatId(),
                    order.getAmount(),
                    order.getStatus(),
                    order.getCreatedAt()
            );
        }
    }
}
