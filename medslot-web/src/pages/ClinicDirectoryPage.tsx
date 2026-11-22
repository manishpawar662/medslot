import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, Clock, Stethoscope, Building2 } from 'lucide-react';
import { httpClient, extractErrorMessage } from '../api/httpClient';
import { useAuth } from '../context/AuthContext';
import type { Clinic, Doctor, TimeSlot } from '../types/api';

export function ClinicDirectoryPage() {
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [selectedClinicId, setSelectedClinicId] = useState<string>('');
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [selectedDoctor, setSelectedDoctor] = useState<Doctor | null>(null);
  const [slots, setSlots] = useState<TimeSlot[]>([]);
  const [reason, setReason] = useState('');
  const [message, setMessage] = useState<{ type: 'ok' | 'err'; text: string } | null>(null);
  const [bookingSlotId, setBookingSlotId] = useState<string | null>(null);

  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    httpClient.get<Clinic[]>('/clinics').then(({ data }) => {
      setClinics(data);
      if (data.length > 0) setSelectedClinicId(data[0].id);
    });
  }, []);

  useEffect(() => {
    if (!selectedClinicId) return;
    setSelectedDoctor(null);
    setSlots([]);
    httpClient.get<Doctor[]>(`/clinics/${selectedClinicId}/doctors`).then(({ data }) => {
      setDoctors(data);
    });
  }, [selectedClinicId]);

  const loadAvailableSlots = async (doctor: Doctor) => {
    setSelectedDoctor(doctor);
    setMessage(null);
    const from = new Date().toISOString();
    const to = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString();
    const { data } = await httpClient.get<TimeSlot[]>('/slots/available', {
      params: { doctorId: doctor.doctorId, from, to },
    });
    setSlots(data);
  };

  const handleBookSlot = async (slotId: string) => {
    if (!user) {
      navigate('/login');
      return;
    }
    setBookingSlotId(slotId);
    setMessage(null);
    try {
      await httpClient.post('/appointments', { slotId, reason: reason || 'General consultation' });
      setMessage({ type: 'ok', text: 'Appointment confirmed!' });
      setSlots((prev) => prev.filter((s) => s.id !== slotId));
      setReason('');
    } catch (err) {
      setMessage({ type: 'err', text: extractErrorMessage(err) });
    } finally {
      setBookingSlotId(null);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Find a Doctor & Book a Slot</h1>
        <p className="text-slate-600 text-sm mt-1">Select a clinic to view specialists and real-time availability.</p>
      </div>

      {/* Clinic Selector */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {clinics.map((c) => (
          <button
            key={c.id}
            onClick={() => setSelectedClinicId(c.id)}
            className={`p-4 rounded-xl border text-left transition ${
              selectedClinicId === c.id
                ? 'border-teal-600 bg-teal-50/60 ring-1 ring-teal-600'
                : 'border-slate-200 bg-white hover:border-slate-300'
            }`}
          >
            <div className="flex items-center gap-2 font-semibold text-slate-900">
              <Building2 className="w-4 h-4 text-teal-600" />
              {c.name}
            </div>
            <div className="text-xs text-slate-500 mt-1">
              {c.code} · {c.timezone}
            </div>
          </button>
        ))}
      </div>

      {/* Doctors List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {doctors.map((doc) => (
          <div
            key={doc.doctorId}
            className={`p-5 rounded-xl border bg-white flex items-center justify-between ${
              selectedDoctor?.doctorId === doc.doctorId ? 'border-teal-600' : 'border-slate-200'
            }`}
          >
            <div>
              <div className="flex items-center gap-2 font-semibold text-slate-900">
                <Stethoscope className="w-4 h-4 text-teal-600" />
                {doc.fullName}
              </div>
              <p className="text-sm text-slate-600 mt-0.5">{doc.specialization}</p>
              <p className="text-xs font-medium text-teal-700 mt-1">Fee: ₹{doc.consultationFee}</p>
            </div>
            <button
              onClick={() => loadAvailableSlots(doc)}
              className="px-4 py-2 text-sm font-medium rounded-lg bg-slate-900 text-white hover:bg-slate-800 transition"
            >
              View Slots
            </button>
          </div>
        ))}
      </div>

      {/* Available Slots Drawer */}
      {selectedDoctor && (
        <div className="bg-white p-6 rounded-xl border border-slate-200 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
              <Calendar className="w-5 h-5 text-teal-600" />
              Available Slots — {selectedDoctor.fullName}
            </h2>
            <input
              type="text"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Visit reason (optional)"
              className="px-3 py-1.5 text-sm border border-slate-300 rounded-lg sm:w-72"
            />
          </div>

          {message && (
            <div
              className={`p-3 rounded-lg text-sm border ${
                message.type === 'ok'
                  ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                  : 'bg-red-50 text-red-700 border-red-200'
              }`}
            >
              {message.text}
            </div>
          )}

          {slots.length === 0 ? (
            <p className="text-sm text-slate-500">No open slots available in the next 14 days.</p>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {slots.map((slot) => (
                <div
                  key={slot.id}
                  className="p-3.5 rounded-lg border border-slate-200 flex items-center justify-between"
                >
                  <div className="text-sm">
                    <div className="font-medium text-slate-900">
                      {new Date(slot.startTime).toLocaleDateString()}
                    </div>
                    <div className="text-slate-600 flex items-center gap-1 text-xs mt-0.5">
                      <Clock className="w-3.5 h-3.5" />
                      {new Date(slot.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} –{' '}
                      {new Date(slot.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>
                  <button
                    disabled={bookingSlotId === slot.id || (user !== null && user.role !== 'PATIENT')}
                    onClick={() => handleBookSlot(slot.id)}
                    className="px-3 py-1.5 text-xs font-semibold rounded-md bg-teal-600 text-white hover:bg-teal-700 disabled:opacity-50"
                  >
                    {bookingSlotId === slot.id ? 'Booking...' : user ? 'Book' : 'Sign in to Book'}
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}