// com/medslot/platform/clinic/ClinicRepository.java
package com.medslot.platform.clinic;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
    Optional<Clinic> findByCode(String code);
}