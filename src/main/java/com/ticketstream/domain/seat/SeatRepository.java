package com.ticketstream.domain.seat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    Optional<Seat> findByIdAndConcertId(Long id, Long concertId);

    List<Seat> findByConcertIdOrderByIdAsc(Long concertId);

    @Modifying
    @Query("UPDATE Seat s SET s.status = com.ticketstream.global.enums.SeatStatus.AVAILABLE WHERE s.concertId = :concertId")
    void resetSeatsByConcertId(@Param("concertId") Long concertId);
}
