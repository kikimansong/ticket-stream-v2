package com.ticketstream.common.enums;

public enum OrderStatus {
    PENDING,    // 가주문 생성 (결제 대기)
    COMPLETED,  // 결제 완료 및 주문 확정
    CANCELLED   // 결제 실패/타임아웃으로 취소됨
}
