// com/medslot/platform/scheduling/TimeSlotRepository.java
package com.medslot.platform.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {

    @Query("""
        SELECT ts FROM TimeSlot ts
        JOIN FETCH ts.doctor d
        JOIN FETCH d.user
        WHERE d.id = :doctorId
          AND ts.status = 'AVAILABLE'
          AND ts.startTime >= :from
          AND ts.startTime < :to
        ORDER BY ts.startTime ASC
        """)
    List<TimeSlot> findAvailableSlots(UUID doctorId, Instant from, Instant to);
    @Query("""
            SELECT s.startTime FROM TimeSlot s
            WHERE s.doctor.id = :doctorId
              AND s.startTime >= :windowStart
              AND s.startTime < :windowEnd
            """)
    Set<Instant> findExistingStartTimes(
            @Param("doctorId") UUID doctorId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );
    
    @Query("""
            SELECT s FROM TimeSlot s
            JOIN FETCH s.clinic
            WHERE s.doctor.id = :doctorId
              AND s.startTime >= :from
              AND s.startTime <= :to
            ORDER BY s.startTime ASC
            """)
    List<TimeSlot> findDoctorSchedule(
            @Param("doctorId") UUID doctorId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}