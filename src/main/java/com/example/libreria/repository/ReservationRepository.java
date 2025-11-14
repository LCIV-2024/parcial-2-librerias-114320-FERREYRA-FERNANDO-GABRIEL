package com.example.libreria.repository;

import com.example.libreria.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    // TODO: Implementar los métodos de la reserva

    // Obtener reservas por ID de usuario
    List<Reservation> findByUserId(Long userId);

    // Obtener reservas por estado (ACTIVE, RETURNED, OVERDUE)
    List<Reservation> findByStatus(Reservation.ReservationStatus status);

    // Obtener reservas vencidas: expected_return_date < hoy AND status = ACTIVE
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.expectedReturnDate < CURRENT_DATE " +
            "AND r.status = com.example.libreria.model.Reservation.ReservationStatus.ACTIVE")
    List<Reservation> findOverdueReservations();
}

