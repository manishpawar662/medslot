import { useEffect, useState, type FormEvent } from 'react';
import { httpClient, extractErrorMessage } from '../api/httpClient';
import type { Clinic } from '../types/api';

export function AdminDashboardPage() {
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [clinicName, setClinicName] = useState('');
  const [clinicCode, setClinicCode] = useState('');
  const [timezone, setTimezone] = useState('Asia/Kolkata');

  const [targetClinicId, setTargetClinicId] = useState('');
  const [docName, setDocName] = useState('');
  const [docEmail, setDocEmail] = useState('');
  const [docPassword, setDocPassword] = useState('');
  const [specialization, setSpecialization] = useState('');
  const [fee, setFee] = useState('700');
  const [banner, setBanner] = useState('');

  const loadClinics = async () => {
    const { data } = await httpClient.get<Clinic[]>('/clinics');
    setClinics(data);
    if (data.length > 0 && !targetClinicId) setTargetClinicId(data[0].id);
  };

  useEffect(() => {
    loadClinics();
  }, []);

  const handleCreateClinic = async (e: FormEvent) => {
    e.preventDefault();
    setBanner('');
    try {
      await httpClient.post('/clinics', { name: clinicName, code: clinicCode, timezone });
      setClinicName('');
      setClinicCode('');
      setBanner('Clinic created successfully.');
      await loadClinics();
    } catch (err) {
      setBanner(extractErrorMessage(err));
    }
  };

  const handleOnboardDoctor = async (e: FormEvent) => {
    e.preventDefault();
    setBanner('');
    try {
      await httpClient.post(`/clinics/${targetClinicId}/doctors`, {
        fullName: docName,
        email: docEmail,
        password: docPassword,
        specialization,
        consultationFee: Number(fee),
      });
      setDocName('');
      setDocEmail('');
      setDocPassword('');
      setSpecialization('');
      setBanner('Doctor onboarded successfully.');
    } catch (err) {
      setBanner(extractErrorMessage(err));
    }
  };

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-bold text-slate-900">Admin Onboarding Console</h1>
      {banner && <div className="p-3 rounded-lg bg-teal-50 text-teal-800 text-sm border border-teal-200">{banner}</div>}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <form onSubmit={handleCreateClinic} className="bg-white p-6 rounded-xl border border-slate-200 space-y-4">
          <h2 className="text-lg font-semibold text-slate-900">1. Create Clinic</h2>
          <input
            type="text"
            required
            placeholder="Clinic Name (e.g., Apollo Indiranagar)"
            value={clinicName}
            onChange={(e) => setClinicName(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
          <input
            type="text"
            required
            placeholder="Clinic Code (e.g., APL-BLR-01)"
            value={clinicCode}
            onChange={(e) => setClinicCode(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
          <input
            type="text"
            required
            placeholder="IANA Timezone (e.g., Asia/Kolkata)"
            value={timezone}
            onChange={(e) => setTimezone(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
          <button className="w-full py-2 bg-slate-900 text-white text-sm font-semibold rounded-lg hover:bg-slate-800">
            Create Clinic
          </button>
        </form>

        <form onSubmit={handleOnboardDoctor} className="bg-white p-6 rounded-xl border border-slate-200 space-y-4">
          <h2 className="text-lg font-semibold text-slate-900">2. Onboard Doctor</h2>
          <select
            value={targetClinicId}
            onChange={(e) => setTargetClinicId(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          >
            {clinics.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} ({c.code})
              </option>
            ))}
          </select>
          <input
            type="text"
            required
            placeholder="Doctor Full Name"
            value={docName}
            onChange={(e) => setDocName(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
          <input
            type="email"
            required
            placeholder="Doctor Email"
            value={docEmail}
            onChange={(e) => setDocEmail(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
          <input
            type="password"
            required
            minLength={8}
            placeholder="Temporary Password (min 8 chars)"
            value={docPassword}
            onChange={(e) => setDocPassword(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
          <div className="grid grid-cols-2 gap-3">
            <input
              type="text"
              required
              placeholder="Specialization"
              value={specialization}
              onChange={(e) => setSpecialization(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
            />
            <input
              type="number"
              required
              min={0}
              placeholder="Fee"
              value={fee}
              onChange={(e) => setFee(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
            />
          </div>
          <button
            disabled={!targetClinicId}
            className="w-full py-2 bg-teal-600 text-white text-sm font-semibold rounded-lg hover:bg-teal-700 disabled:opacity-50"
          >
            Onboard Doctor
          </button>
        </form>
      </div>
    </div>
  );
}