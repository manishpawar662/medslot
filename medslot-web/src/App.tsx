import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { ClinicDirectoryPage } from './pages/ClinicDirectoryPage';
import { PatientAppointmentsPage } from './pages/PatientAppointmentsPage';
import { DoctorSchedulePage } from './pages/DoctorSchedulePage';
import { AdminDashboardPage } from './pages/AdminDashboardPage';

function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const defaultRouteForRole =
    user?.role === 'ADMIN' ? '/admin' : user?.role === 'DOCTOR' ? '/doctor' : '/';

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-5xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-6">
            <Link to="/" className="text-xl font-bold text-teal-700 tracking-tight">
              MedSlot
            </Link>
            <nav className="flex items-center gap-4 text-sm font-medium text-slate-600">
              <Link to="/" className="hover:text-teal-700">Clinics</Link>
              {user?.role === 'PATIENT' && (
                <Link to="/appointments" className="hover:text-teal-700">My Appointments</Link>
              )}
              {user?.role === 'DOCTOR' && (
                <Link to="/doctor" className="hover:text-teal-700">My Schedule</Link>
              )}
              {user?.role === 'ADMIN' && (
                <Link to="/admin" className="hover:text-teal-700">Admin Console</Link>
              )}
            </nav>
          </div>

          <div className="flex items-center gap-3">
            {user ? (
              <>
                <span className="text-xs text-slate-500 hidden sm:inline">
                  {user.email} ({user.role})
                </span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-slate-300 hover:bg-slate-100"
                >
                  Sign Out
                </button>
              </>
            ) : (
              <Link
                to="/login"
                className="px-4 py-1.5 text-xs font-semibold rounded-lg bg-teal-600 text-white hover:bg-teal-700"
              >
                Sign In
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-4 py-8">
        <Routes>
          <Route path="/" element={<ClinicDirectoryPage />} />
          <Route
            path="/login"
            element={user ? <Navigate to={defaultRouteForRole} replace /> : <LoginPage />}
          />
          <Route
            path="/appointments"
            element={user?.role === 'PATIENT' ? <PatientAppointmentsPage /> : <Navigate to="/login" replace />}
          />
          <Route
            path="/doctor"
            element={user?.role === 'DOCTOR' ? <DoctorSchedulePage /> : <Navigate to="/login" replace />}
          />
          <Route
            path="/admin"
            element={user?.role === 'ADMIN' ? <AdminDashboardPage /> : <Navigate to="/login" replace />}
          />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Layout />
      </BrowserRouter>
    </AuthProvider>
  );
}