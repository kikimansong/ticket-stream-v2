package com.ticketstream.domain.seat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    Optional<Seat> findByIdAndConcertId(Long Id, Long concertId);

    List<Seat> findByConcertIdOrderByIdAsc(Long concertId);

}
