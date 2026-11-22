// com/medslot/platform/booking/AppointmentRepository.java
package com.medslot.platform.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.slot s
        JOIN FETCH s.doctor d
        JOIN FETCH d.user
        JOIN FETCH a.clinic
        WHERE a.id = :id
        """)
    Optional<Appointment> findByIdWithDetails(UUID id);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.slot s
            JOIN FETCH s.doctor d
            JOIN FETCH d.user
            JOIN FETCH a.clinic
            WHERE a.patient.id = :patientId
            ORDER BY s.startTime DESC
            """)
    List<Appointment> findAllByPatientIdWithDetails(@Param("patientId") UUID patientId);
    
    
    
    
}