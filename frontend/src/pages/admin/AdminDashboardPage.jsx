import { useEffect, useState } from 'react'
import { adminApi } from '../../api/endpoints'
import StatCard from '../../components/ui/StatCard'
import { PageLoader } from '../../components/ui/Loaders'
import { Link } from 'react-router-dom'
import {
  Users, Package, CheckCircle, XCircle, Clock, TrendingUp,
  RotateCcw, Search, PlusCircle, AlertCircle, Bell
} from 'lucide-react'

export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    adminApi.getDashboard()
      .then(r => setStats(r.data.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <PageLoader />

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">College Admin Dashboard</h1>
          <p className="page__sub">Official Found Items, Student Lost Reports &amp; Ownership Verification</p>
        </div>
        <div className="header-actions">
          <Link to="/admin/found-items" className="btn btn--primary">
            <PlusCircle size={16} /> Add Found Item
          </Link>
        </div>
      </div>

      {/* Stats grid */}
      <div className="stats-grid">
        <StatCard icon={TrendingUp}  label="Total Found Items"   value={stats?.foundItems}     color="teal" />
        <StatCard icon={Search}      label="Total Lost Reports"  value={stats?.lostItems}      color="red" />
        <StatCard icon={Clock}       label="Pending Claims"      value={stats?.pendingClaims}  color="orange" />
        <StatCard icon={CheckCircle} label="Approved Claims"     value={stats?.approvedClaims} color="green" />
        <StatCard icon={XCircle}     label="Rejected Claims"     value={stats?.rejectedClaims} color="red" />
        <StatCard icon={RotateCcw}   label="Returned Items"      value={stats?.returnedItems}  color="blue" />
        <StatCard icon={Package}     label="Available Items"     value={stats?.openItems}      color="purple" />
        <StatCard icon={Users}       label="Registered Users"    value={stats?.totalUsers}     color="blue" />
      </div>

      {/* Quick links */}
      <div className="admin-quick-links">
        <h2 className="admin-quick-links__title">Management Sections</h2>
        <div className="admin-quick-links__grid">
          <Link to="/admin/found-items" className="quick-link quick-link--purple">
            <Package size={24} />
            <div>
              <div className="quick-link__val">{stats?.foundItems ?? 0}</div>
              <div className="quick-link__label">Manage Found Items</div>
            </div>
          </Link>
          <Link to="/admin/lost-reports" className="quick-link quick-link--red">
            <AlertCircle size={24} />
            <div>
              <div className="quick-link__val">{stats?.lostItems ?? 0}</div>
              <div className="quick-link__label">View Lost Reports</div>
            </div>
          </Link>
          <Link to="/admin/claims?status=PENDING" className="quick-link quick-link--orange">
            <Clock size={24} />
            <div>
              <div className="quick-link__val">{stats?.pendingClaims ?? 0}</div>
              <div className="quick-link__label">Pending Claim Requests</div>
            </div>
          </Link>
          <Link to="/admin/claims?status=APPROVED" className="quick-link quick-link--green">
            <RotateCcw size={24} />
            <div>
              <div className="quick-link__val">{stats?.claimedItems ?? 0}</div>
              <div className="quick-link__label">Awaiting Handover / Return</div>
            </div>
          </Link>
          <Link to="/admin/users" className="quick-link quick-link--blue">
            <Users size={24} />
            <div>
              <div className="quick-link__val">{stats?.totalUsers ?? 0}</div>
              <div className="quick-link__label">Registered Students</div>
            </div>
          </Link>
          <Link to="/notifications" className="quick-link quick-link--teal">
            <Bell size={24} />
            <div>
              <div className="quick-link__val">Inbox</div>
              <div className="quick-link__label">Admin Notifications</div>
            </div>
          </Link>
        </div>
      </div>
    </div>
  )
}
