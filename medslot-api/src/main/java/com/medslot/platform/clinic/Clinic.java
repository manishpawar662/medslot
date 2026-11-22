// com/medslot/platform/clinic/Clinic.java
package com.medslot.platform.clinic;

import com.medslot.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clinics")
@Getter
@Setter
@NoArgsConstructor
public class Clinic extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(nullable = false)
    private boolean active = true;
}