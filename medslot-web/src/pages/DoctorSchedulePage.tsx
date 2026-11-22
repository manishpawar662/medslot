import { useEffect, useState, type FormEvent } from 'react';
import { httpClient, extractErrorMessage } from '../api/httpClient';
import { useAuth } from '../context/AuthContext';
import type { Clinic, Doctor, DoctorScheduleSlot } from '../types/api';

export function DoctorSchedulePage() {
  const { user } = useAuth();
  const [doctorId, setDoctorId] = useState<string>('');
  const [schedule, setSchedule] = useState<DoctorScheduleSlot[]>([]);
  const [windowStart, setWindowStart] = useState('');
  const [windowEnd, setWindowEnd] = useState('');
  const [duration, setDuration] = useState(30);
  const [statusMsg, setStatusMsg] = useState('');

  const loadSchedule = async () => {
    const from = new Date().toISOString();
    const to = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString();
    const { data } = await httpClient.get<DoctorScheduleSlot[]>('/slots/my-schedule', {
      params: { from, to },
    });
    setSchedule(data);
  };

  useEffect(() => {
    loadSchedule();
    // Resolve doctorId from clinics directory for bulk slot generation
    httpClient.get<Clinic[]>('/clinics').then(async ({ data: clinics }) => {
      for (const c of clinics) {
        const { data: docs } = await httpClient.get<Doctor[]>(`/clinics/${c.id}/doctors`);
        const match = docs.find((d) => d.userId === user?.userId);
        if (match) {
          setDoctorId(match.doctorId);
          break;
        }
      }
    });
  }, [user?.userId]);

  const handleBulkCreate = async (e: FormEvent) => {
    e.preventDefault();
    setStatusMsg('');
    try {
      const { data } = await httpClient.post('/slots/bulk', {
        doctorId,
        windowStart: new Date(windowStart).toISOString(),
        windowEnd: new Date(windowEnd).toISOString(),
        slotDurationMinutes: duration,
      });
      setStatusMsg(`Generated ${data.length} new slots.`);
      await loadSchedule();
    } catch (err) {
      setStatusMsg(extractErrorMessage(err));
    }
  };

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-bold text-slate-900">Doctor Schedule & Slot Generator</h1>

      <form
        onSubmit={handleBulkCreate}
        className="bg-white p-6 rounded-xl border border-slate-200 grid grid-cols-1 sm:grid-cols-4 gap-4 items-end"
      >
        <div>
          <label className="block text-xs font-medium text-slate-700 mb-1">Shift Start</label>
          <input
            type="datetime-local"
            required
            value={windowStart}
            onChange={(e) => setWindowStart(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-700 mb-1">Shift End</label>
          <input
            type="datetime-local"
            required
            value={windowEnd}
            onChange={(e) => setWindowEnd(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-700 mb-1">Duration (mins)</label>
          <select
            value={duration}
            onChange={(e) => setDuration(Number(e.target.value))}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          >
            <option value={15}>15 mins</option>
            <option value={30}>30 mins</option>
            <option value={45}>45 mins</option>
            <option value={60}>60 mins</option>
          </select>
        </div>
        <button
          type="submit"
          disabled={!doctorId}
          className="py-2 px-4 bg-teal-600 hover:bg-teal-700 text-white text-sm font-semibold rounded-lg disabled:opacity-50"
        >
          Generate Slots
        </button>
      </form>

      {statusMsg && <p className="text-sm font-medium text-teal-700">{statusMsg}</p>}

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {schedule.map((slot) => (
          <div key={slot.slotId} className="p-4 bg-white rounded-xl border border-slate-200">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-slate-900">
                {new Date(slot.startTime).toLocaleDateString()}
              </span>
              <span
                className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                  slot.status === 'AVAILABLE'
                    ? 'bg-emerald-100 text-emerald-800'
                    : 'bg-amber-100 text-amber-800'
                }`}
              >
                {slot.status}
              </span>
            </div>
            <p className="text-xs text-slate-600 mt-1">
              {new Date(slot.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} –{' '}
              {new Date(slot.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}