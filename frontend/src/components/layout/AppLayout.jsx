import { useState, useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { notificationApi } from '../../api/endpoints'
import {
  LayoutDashboard, Search, FileText, Bell,
  LogOut, Menu, X, Shield, Users,
  Package, ClipboardList, ChevronRight, AlertCircle, CheckSquare
} from 'lucide-react'

const userNavItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/items', icon: Search, label: 'Search Found Items' },
  { to: '/items/new', icon: AlertCircle, label: 'Report Lost Item' },
  { to: '/my-items', icon: Package, label: 'My Lost Reports' },
  { to: '/my-claims', icon: FileText, label: 'My Claim Requests' },
  { to: '/notifications', icon: Bell, label: 'Notifications' },
]

const adminNavItems = [
  { to: '/admin/dashboard', icon: Shield, label: 'Admin Dashboard' },
  { to: '/admin/found-items', icon: CheckSquare, label: 'Manage Found Items' },
  { to: '/admin/lost-reports', icon: AlertCircle, label: 'Lost Reports' },
  { to: '/admin/claims', icon: ClipboardList, label: 'Claim Requests' },
  { to: '/admin/users', icon: Users, label: 'Users' },
  { to: '/notifications', icon: Bell, label: 'Notifications' },
]

export default function AppLayout({ children }) {
  const { user, logout, isAdmin } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    if (user) {
      notificationApi.getUnreadCount()
        .then(r => setUnreadCount(r.data.data.unreadCount))
        .catch(() => {})
    }
  }, [user, location.pathname])

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  const isActive = (path) => {
    if (path === '/items') {
      return location.pathname === '/items' || /^\/items\/\d+$/.test(location.pathname)
    }
    return location.pathname === path || location.pathname.startsWith(path + '/')
  }

  const navItems = isAdmin ? adminNavItems : userNavItems

  return (
    <div className="app-layout">
      {sidebarOpen && (
        <div className="sidebar-overlay" onClick={() => setSidebarOpen(false)} />
      )}

      <aside className={`sidebar ${sidebarOpen ? 'sidebar--open' : ''}`}>
        <div className="sidebar__header">
          <Link to="/" className="sidebar__logo">
            <div className="sidebar__logo-icon">
              <Search size={20} />
            </div>
            <span>College L&amp;F Hub</span>
          </Link>
          <button className="sidebar__close" onClick={() => setSidebarOpen(false)}>
            <X size={20} />
          </button>
        </div>

        <nav className="sidebar__nav">
          <div className="sidebar__section-label">
            {isAdmin ? 'Administration' : 'Main Menu'}
          </div>
          {navItems.map(({ to, icon: Icon, label }) => (
            <Link
              key={to}
              to={to}
              className={`sidebar__link ${isAdmin ? 'sidebar__link--admin' : ''} ${isActive(to) ? 'sidebar__link--active' : ''}`}
              onClick={() => setSidebarOpen(false)}
            >
              <Icon size={18} />
              <span>{label}</span>
              {label === 'Notifications' && unreadCount > 0 && (
                <span className="badge badge--red">{unreadCount > 99 ? '99+' : unreadCount}</span>
              )}
              {isActive(to) && <ChevronRight size={14} className="sidebar__chevron" />}
            </Link>
          ))}
        </nav>

        <div className="sidebar__footer">
          <div className="sidebar__user">
            <div className="avatar avatar--sm">{user?.name?.[0]?.toUpperCase()}</div>
            <div className="sidebar__user-info">
              <span className="sidebar__user-name">{user?.name}</span>
              <span className="sidebar__user-role">{user?.role === 'ADMIN' ? 'College Admin' : 'Student'}</span>
            </div>
          </div>
          <button className="sidebar__logout" onClick={handleLogout} title="Logout">
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      <div className="main-content">
        <header className="topbar">
          <button className="topbar__menu-btn" onClick={() => setSidebarOpen(true)}>
            <Menu size={22} />
          </button>
          <div className="topbar__brand">
            <Link to="/" className="topbar__logo">
              <Search size={18} />
              <span>College L&amp;F Hub</span>
            </Link>
          </div>
          <div className="topbar__actions">
            <Link to="/notifications" className="topbar__icon-btn">
              <Bell size={20} />
              {unreadCount > 0 && <span className="topbar__badge">{unreadCount > 9 ? '9+' : unreadCount}</span>}
            </Link>
            <div className="topbar__avatar">
              <div className="avatar avatar--sm">{user?.name?.[0]?.toUpperCase()}</div>
            </div>
          </div>
        </header>

        <main className="page-content">
          {children}
        </main>
      </div>
    </div>
  )
}
