export type UserRole = 'PATIENT' | 'DOCTOR' | 'ADMIN';
export type SlotStatus = 'AVAILABLE' | 'BOOKED' | 'BLOCKED';
export type AppointmentStatus = 'CONFIRMED' | 'CANCELLED';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: string;
  email: string;
  role: UserRole;
}

export interface Clinic {
  id: string;
  name: string;
  code: string;
  timezone: string;
}

export interface Doctor {
  doctorId: string;
  userId: string;
  clinicId: string;
  fullName: string;
  email: string;
  specialization: string;
  consultationFee: number;
}

export interface TimeSlot {
  id: string;
  doctorId: string;
  clinicId: string;
  startTime: string;
  endTime: string;
  status: SlotStatus;
}

export interface PatientAppointmentView {
  appointmentId: string;
  status: AppointmentStatus;
  reason: string | null;
  slotId: string;
  startTime: string;
  endTime: string;
  doctorName: string;
  specialization: string;
  consultationFee: number;
  clinicName: string;
}

export interface DoctorScheduleSlot {
  slotId: string;
  startTime: string;
  endTime: string;
  status: SlotStatus;
  clinicName: string;
}