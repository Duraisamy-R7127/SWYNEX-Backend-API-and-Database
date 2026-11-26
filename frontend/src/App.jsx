import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { Search } from 'lucide-react'
import { AuthProvider, useAuth } from './context/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import AppLayout from './components/layout/AppLayout'

import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import ItemsPage from './pages/ItemsPage'
import ItemDetailPage from './pages/ItemDetailPage'
import ItemFormPage from './pages/ItemFormPage'
import MyItemsPage from './pages/MyItemsPage'
import MyClaimsPage from './pages/MyClaimsPage'
import NotificationsPage from './pages/NotificationsPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminLoginPage from './pages/admin/AdminLoginPage'
import AdminFoundItemsPage from './pages/admin/AdminFoundItemsPage'
import AdminLostReportsPage from './pages/admin/AdminLostReportsPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'
import AdminItemsPage from './pages/admin/AdminItemsPage'
import AdminClaimsPage from './pages/admin/AdminClaimsPage'

function WithLayout({ children }) {
  return (
    <ProtectedRoute>
      <AppLayout>{children}</AppLayout>
    </ProtectedRoute>
  )
}

function PublicOrAppLayout({ children }) {
  const { user } = useAuth()
  if (user) {
    return <AppLayout>{children}</AppLayout>
  }
  return (
    <div className="public-shell">
      <nav className="landing__nav">
        <div className="landing__nav-inner">
          <Link to="/" className="landing__nav-logo">
            <Search size={22} />
            <span>College L&amp;F Hub</span>
          </Link>
          <div className="landing__nav-links">
            <Link to="/items" className="landing__nav-link">Found Items</Link>
            <Link to="/admin/login" className="landing__nav-link">Admin</Link>
            <Link to="/login" className="btn btn--ghost">Sign In</Link>
            <Link to="/register" className="btn btn--primary">Register</Link>
          </div>
        </div>
      </nav>
      <main className="public-shell__main">{children}</main>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Toaster position="top-right" toastOptions={{ duration: 3500, style: { borderRadius: '10px', fontFamily: 'Inter, sans-serif' } }} />
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/admin/login" element={<AdminLoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route path="/dashboard" element={<WithLayout><DashboardPage /></WithLayout>} />
          <Route path="/items" element={<PublicOrAppLayout><ItemsPage /></PublicOrAppLayout>} />
          <Route path="/items/new" element={<WithLayout><ItemFormPage /></WithLayout>} />
          <Route path="/items/:id" element={<PublicOrAppLayout><ItemDetailPage /></PublicOrAppLayout>} />
          <Route path="/items/:id/edit" element={<WithLayout><ItemFormPage /></WithLayout>} />
          <Route path="/my-items" element={<WithLayout><MyItemsPage /></WithLayout>} />
          <Route path="/my-claims" element={<WithLayout><MyClaimsPage /></WithLayout>} />
          <Route path="/notifications" element={<WithLayout><NotificationsPage /></WithLayout>} />

          <Route path="/admin/dashboard" element={<ProtectedRoute adminOnly><AppLayout><AdminDashboardPage /></AppLayout></ProtectedRoute>} />
          <Route path="/admin/found-items" element={<ProtectedRoute adminOnly><AppLayout><AdminFoundItemsPage /></AppLayout></ProtectedRoute>} />
          <Route path="/admin/lost-reports" element={<ProtectedRoute adminOnly><AppLayout><AdminLostReportsPage /></AppLayout></ProtectedRoute>} />
          <Route path="/admin/users" element={<ProtectedRoute adminOnly><AppLayout><AdminUsersPage /></AppLayout></ProtectedRoute>} />
          <Route path="/admin/items" element={<ProtectedRoute adminOnly><AppLayout><AdminItemsPage /></AppLayout></ProtectedRoute>} />
          <Route path="/admin/claims" element={<ProtectedRoute adminOnly><AppLayout><AdminClaimsPage /></AppLayout></ProtectedRoute>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
