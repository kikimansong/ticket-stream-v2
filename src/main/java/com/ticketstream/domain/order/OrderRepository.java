package com.ticketstream.domain.order;

import com.ticketstream.global.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findFirstBySeatIdAndStatusOrderByCreatedAtDesc(Long seatId, OrderStatus status);
}
