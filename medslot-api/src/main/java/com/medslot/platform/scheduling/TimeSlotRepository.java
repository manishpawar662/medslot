// com/medslot/platform/scheduling/TimeSlotRepository.java
package com.medslot.platform.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
}