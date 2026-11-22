-- 1. Clinics (Multi-tenant boundary)
CREATE TABLE clinics (
    id          UUID PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    timezone    VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 2. Users (Patients, Doctors, Admins)
CREATE TABLE users (
    id             UUID PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    role           VARCHAR(30)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_role CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN'))
);

-- 3. Doctors (Links a User to a Clinic & Specialty)
CREATE TABLE doctors (
    id                UUID PRIMARY KEY,
    user_id           UUID           NOT NULL UNIQUE REFERENCES users(id),
    clinic_id         UUID           NOT NULL REFERENCES clinics(id),
    specialization    VARCHAR(100)   NOT NULL,
    consultation_fee  NUMERIC(10, 2) NOT NULL CHECK (consultation_fee >= 0),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doctors_clinic_id ON doctors(clinic_id);

-- 4. Time Slots (Concurrency & Optimistic Locking target)
CREATE TABLE time_slots (
    id          UUID PRIMARY KEY,
    doctor_id   UUID        NOT NULL REFERENCES doctors(id),
    clinic_id   UUID        NOT NULL REFERENCES clinics(id),
    start_time  TIMESTAMPTZ NOT NULL,
    end_time    TIMESTAMPTZ NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_slot_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_slot_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'BLOCKED')),
    CONSTRAINT uq_doctor_slot_start UNIQUE (doctor_id, start_time)
);

CREATE INDEX idx_time_slots_doctor_status_start ON time_slots(doctor_id, status, start_time);

-- 5. Appointments (Booking lifecycle + PostgreSQL Partial Unique Index)
CREATE TABLE appointments (
    id            UUID PRIMARY KEY,
    slot_id       UUID        NOT NULL REFERENCES time_slots(id),
    patient_id    UUID        NOT NULL REFERENCES users(id),
    clinic_id     UUID        NOT NULL REFERENCES clinics(id),
    status        VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    reason        VARCHAR(500),
    cancelled_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_appointment_status CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED'))
);

-- Guarantees at the DB level that at most ONE active CONFIRMED appointment can exist per slot,
-- while allowing historical CANCELLED appointments for the same slot_id.
CREATE UNIQUE INDEX uq_active_slot_booking
    ON appointments(slot_id)
    WHERE status = 'CONFIRMED';

CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_clinic_id ON appointments(clinic_id);