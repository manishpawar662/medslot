import { useEffect, useState } from 'react';
import { httpClient, extractErrorMessage } from '../api/httpClient';
import type { PatientAppointmentView } from '../types/api';

export function PatientAppointmentsPage() {
  const [appointments, setAppointments] = useState<PatientAppointmentView[]>([]);
  const [error, setError] = useState('');

  const fetchAppointments = async () => {
    try {
      const { data } = await httpClient.get<PatientAppointmentView[]>('/appointments/my');
      setAppointments(data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  useEffect(() => {
    fetchAppointments();
  }, []);

  const handleCancel = async (appointmentId: string) => {
    try {
      await httpClient.patch(`/appointments/${appointmentId}/cancel`);
      await fetchAppointments();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">My Appointments</h1>
      {error && <div className="p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

      {appointments.length === 0 ? (
        <p className="text-slate-500 text-sm">You have no appointments yet.</p>
      ) : (
        <div className="space-y-3">
          {appointments.map((appt) => (
            <div
              key={appt.appointmentId}
              className="p-5 bg-white rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-slate-900">{appt.doctorName}</span>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-700">
                    {appt.specialization}
                  </span>
                  <span
                    className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                      appt.status === 'CONFIRMED'
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-slate-200 text-slate-600'
                    }`}
                  >
                    {appt.status}
                  </span>
                </div>
                <p className="text-sm text-slate-600 mt-1">
                  {appt.clinicName} · {new Date(appt.startTime).toLocaleString()}
                </p>
                {appt.reason && <p className="text-xs text-slate-500 mt-1">Reason: {appt.reason}</p>}
              </div>

              {appt.status === 'CONFIRMED' && (
                <button
                  onClick={() => handleCancel(appt.appointmentId)}
                  className="px-4 py-2 text-xs font-semibold rounded-lg border border-red-200 text-red-700 hover:bg-red-50"
                >
                  Cancel Appointment
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}