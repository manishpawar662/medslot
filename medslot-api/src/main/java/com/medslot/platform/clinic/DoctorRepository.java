// DoctorRepository.java
package com.medslot.platform.clinic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    @Query("""
            SELECT d FROM Doctor d
            JOIN FETCH d.user
            JOIN FETCH d.clinic
            WHERE d.clinic.id = :clinicId
            """)
    List<Doctor> findAllByClinicIdWithDetails(@Param("clinicId") UUID clinicId);

    Optional<Doctor> findByUserId(UUID userId);
}