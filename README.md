# MedSlot — Healthcare Clinic & Appointment Booking Platform

**MedSlot** is a full-stack, concurrency-safe healthcare scheduling and appointment platform built as a single monorepo (`medslot-api` + `medslot-web`). It enables clinic administrators to onboard healthcare facilities and practitioners, doctors to generate shift availability idempotently, and patients to discover doctors and book time slots safely under concurrent traffic.

---

## Key Architectural Highlights

* **Monorepo Architecture:** Houses the Spring Boot REST API (`medslot-api`) and React + TypeScript SPA (`medslot-web`) under one repository with unified Git hygiene.
* **Concurrency-Safe Booking Engine:** Prevents double-booking race conditions by combining pessimistic database row locking (`SELECT ... FOR UPDATE` via `@Lock(LockModeType.PESSIMISTIC_WRITE)`) with database-level unique constraints (`uq_doctor_slot_start`).
* **Stateless JWT & Role-Based Access Control (RBAC):** Custom `JwtAuthenticationFilter` and Spring Security method/URL rules enforcing strict boundaries across `PATIENT`, `DOCTOR`, and `ADMIN` roles.
* **N+1 Query Prevention:** Uses targeted JPQL `JOIN FETCH` queries for clinic doctor directories, patient appointment histories, and doctor schedule views.
* **Idempotent Bulk Slot Generation:** Slices multi-hour doctor shifts into discrete intervals while skipping already-existing timestamps so repeated requests never fail or duplicate slots.
* **Schema Evolution & Real-DB Integration Testing:** Versioned PostgreSQL migrations via Flyway (`V1__init_schema.sql`) and multi-threaded integration tests backed by Testcontainers.

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend (`medslot-api`)** | Java 21, Spring Boot, Spring Security (JWT), Spring Data JPA / Hibernate, Flyway, Springdoc OpenAPI (Swagger UI), Spring Boot Actuator |
| **Frontend (`medslot-web`)** | React 19, TypeScript, Vite, Tailwind CSS v4, React Router, Axios, Lucide Icons |
| **Database & Testing** | PostgreSQL, Testcontainers, JUnit 5, MockMvc |

---

## Monorepo Structure

```text
MedSlot/
├── medslot-api/                              # Spring Boot REST API
│   ├── src/main/java/com/medslot/platform/
│   │   ├── identity/                         # Auth, JWT provider/filter, RBAC, Admin seeder
│   │   ├── clinic/                           # Clinic & doctor onboarding, timezone validation
│   │   ├── scheduling/                       # Single & idempotent bulk slot generation, doctor schedule
│   │   ├── booking/                          # Pessimistic-locked booking, cancellation, patient history
│   │   └── common/                           # BaseEntity, GlobalExceptionHandler, OpenAPI config
│   ├── src/main/resources/
│   │   ├── application.yaml                  # Server, datasource, JWT, and Actuator configuration
│   │   └── db/migration/V1__init_schema.sql  # Flyway PostgreSQL schema
│   ├── src/test/java/com/medslot/platform/   # Testcontainers security, concurrency & onboarding tests
│   └── pom.xml
├── medslot-web/                              # React + TypeScript + Vite SPA
│   ├── src/
│   │   ├── api/httpClient.ts                 # Axios instance with JWT request & 401 response interceptors
│   │   ├── context/AuthContext.tsx           # Persistent auth state & role management
│   │   ├── pages/                            # Login, ClinicDirectory, PatientAppointments, DoctorSchedule, AdminDashboard
│   │   ├── types/api.ts                      # TypeScript contracts mirroring backend DTOs
│   │   └── App.tsx                           # Role-guarded routes & navigation layout
│   ├── package.json
│   └── vite.config.ts
└── README.md
```

---

## Getting Started

### Prerequisites

* **Java 21** & **Maven** (or the included `./mvnw` wrapper)
* **PostgreSQL 15+** running locally (plus **Docker** if running Testcontainers integration tests)
* **Node.js 20+** & **npm**

### 1. Backend Setup (`medslot-api`)

1. Ensure PostgreSQL is running with a database named `medslot` (or update `medslot-api/src/main/resources/application.yaml`):
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/medslot
       username: postgres
       password: your_password
   ```
2. Start the Spring Boot API:
   ```bash
   cd medslot-api
   ./mvnw spring-boot:run
   ```
3. The API starts at **`http://localhost:8080`**. On first boot, `AdminBootstrapRunner` idempotently seeds a default administrator account:
   * **Email:** `admin@medslot.io`
   * **Password:** `AdminPass@123`

### 2. Frontend Setup (`medslot-web`)

1. Install dependencies and start the Vite development server:
   ```bash
   cd medslot-web
   npm install
   npm run dev
   ```
2. Open **`http://localhost:5173`** in your browser.

---

## API Documentation & Endpoints Reference

* **Interactive Swagger UI:** `http://localhost:8080/swagger-ui.html` *(click **Authorize** and paste your JWT token)*
* **OpenAPI Spec (JSON):** `http://localhost:8080/v3/api-docs`
* **Health Probe:** `http://localhost:8080/actuator/health`

All endpoints are prefixed with **`/api/v1`**:

| Module | Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/auth/register` | Public | Register a new `PATIENT` account |
| | `POST` | `/auth/login` | Public | Authenticate and receive a JWT access token |
| **Clinics** | `GET` | `/clinics` | Public | List all registered clinics |
| | `POST` | `/clinics` | `ADMIN` | Create a new clinic with IANA timezone validation |
| | `GET` | `/clinics/{clinicId}/doctors` | Public | List doctors practicing at a clinic |
| | `POST` | `/clinics/{clinicId}/doctors` | `ADMIN` | Atomically create a `DOCTOR` user and profile |
| **Scheduling** | `GET` | `/slots/available` | Public | Query open slots for a doctor within a time range |
| | `POST` | `/slots` | `DOCTOR`, `ADMIN` | Create a single time slot |
| | `POST` | `/slots/bulk` | `DOCTOR`, `ADMIN` | Idempotently generate slots across a shift window |
| | `GET` | `/slots/my-schedule` | `DOCTOR` | View authenticated doctor's upcoming schedule |
| **Booking** | `POST` | `/appointments` | `PATIENT` | Book an available slot (pessimistically locked) |
| | `GET` | `/appointments/my` | `PATIENT` | View authenticated patient's appointment history |
| | `PATCH` | `/appointments/{id}/cancel` | `PATIENT` | Cancel a confirmed appointment and release the slot |

---

## Running Integration Tests

Ensure Docker is running (required for Testcontainers PostgreSQL), then run:

```bash
cd medslot-api
./mvnw clean test
```

The integration test suite verifies:
1. **Authentication & Security Boundaries (`AuthAndSecurityIntegrationTest`):** Password hashing, JWT issuance, `401` unauthenticated rejection, and `403` role-boundary enforcement.
2. **Concurrent Booking Race Conditions (`BookingConcurrencyIntegrationTest`):** Spawns concurrent threads racing for the exact same time slot and asserts that strictly **1** booking succeeds (`201 Created`) while all competing requests fail cleanly (`409 Conflict`).
3. **Onboarding & Idempotent Bulk Scheduling (`ClinicOnboardingAndBulkSlotIntegrationTest`):** Verifies clinic creation, doctor onboarding, and idempotent bulk slot generation without duplicates.

---

## End-to-End Walkthrough

1. **Admin Onboarding:** Sign in at `/login` as `admin@medslot.io` / `AdminPass@123`, open **Admin Console**, create a clinic (e.g., `Asia/Kolkata`), and onboard a doctor.
2. **Doctor Shift Generation:** Sign out, sign in with the newly created doctor credentials, and use **My Schedule** to bulk-generate 30-minute consultation slots.
3. **Patient Discovery & Booking:** Sign up as a new patient, browse clinics on the home page, select the doctor, book an open slot, and manage or cancel the reservation under **My Appointments**.