// com/medslot/platform/clinic/DoctorRepository.java
package com.medslot.platform.clinic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    @Query("""
        SELECT d FROM Doctor d
        JOIN FETCH d.user
        JOIN FETCH d.clinic
        WHERE d.clinic.id = :clinicId
        """)
    List<Doctor> findAllByClinicIdWithDetails(UUID clinicId);
}