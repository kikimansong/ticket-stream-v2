package com.ticketstream.common.enums;

public enum SeatStatus {
    AVAILABLE,  // 예매 가능
    RESERVED,   // 임시 선점 (5분 유효)
    SOLD        // 결제 완료 및 판매 확정
}
