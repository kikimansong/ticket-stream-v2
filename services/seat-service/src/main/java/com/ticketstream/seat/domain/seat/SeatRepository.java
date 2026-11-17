package com.ticketstream.seat.domain.seat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByConcertIdOrderByIdAsc(Long concertId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE seats SET status = 'AVAILABLE', version = 0 WHERE concert_id = :concertId", nativeQuery = true)
    int resetSeatsByConcertId(@Param("concertId") Long concertId);
}
