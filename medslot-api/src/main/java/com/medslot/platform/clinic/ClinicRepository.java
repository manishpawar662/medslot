// ClinicRepository.java
package com.medslot.platform.clinic;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
    boolean existsByCode(String code);
}