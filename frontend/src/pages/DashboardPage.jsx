import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { userApi, notificationApi, itemApi } from '../api/endpoints'
import { PageLoader } from '../components/ui/Loaders'
import ItemCard from '../components/ui/ItemCard'
import { Package, FileText, Bell, AlertCircle, ArrowRight, CheckCircle, Clock, XCircle, Search, Sparkles } from 'lucide-react'

const CLAIM_STATUS_ICON = { PENDING: Clock, APPROVED: CheckCircle, REJECTED: XCircle }
const CLAIM_STATUS_COLOR = { PENDING: 'yellow', APPROVED: 'green', REJECTED: 'red' }

export default function DashboardPage() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [matches, setMatches] = useState([])
  const [claims, setClaims] = useState([])
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      userApi.getMyItems(0, 4),
      userApi.getMyClaims(),
      notificationApi.getNotifications(0, 5),
      itemApi.getMatches().catch(() => ({ data: { data: [] } })),
    ]).then(([itemsRes, claimsRes, notifRes, matchesRes]) => {
      setItems(itemsRes.data.data.content || [])
      setClaims(claimsRes.data.data || [])
      setNotifications(notifRes.data.data.content || [])
      setMatches(matchesRes.data?.data || [])
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  if (loading) return <PageLoader />

  const pendingClaims = claims.filter(c => c.status === 'PENDING').length
  const approvedClaims = claims.filter(c => c.status === 'APPROVED').length

  return (
    <div className="page">
      {/* Welcome */}
      <div className="page__header">
        <div>
          <h1 className="page__title">Welcome, {user?.name?.split(' ')[0]}! 👋</h1>
          <p className="page__sub">Search the official found items board or report a lost item below.</p>
        </div>
        <div className="header-actions">
          <Link to="/items" className="btn btn--outline">
            <Search size={16} /> Search Found Items
          </Link>
          <Link to="/items/new" className="btn btn--primary">
            <AlertCircle size={16} /> Report Lost Item
          </Link>
        </div>
      </div>

      {/* Quick stats */}
      <div className="dashboard-stats">
        <div className="dash-stat dash-stat--blue">
          <Package size={22} />
          <div>
            <div className="dash-stat__val">{items.length}</div>
            <div className="dash-stat__label">My Lost Reports</div>
          </div>
        </div>
        <div className="dash-stat dash-stat--purple">
          <FileText size={22} />
          <div>
            <div className="dash-stat__val">{claims.length}</div>
            <div className="dash-stat__label">Total Claims</div>
          </div>
        </div>
        <div className="dash-stat dash-stat--yellow">
          <Clock size={22} />
          <div>
            <div className="dash-stat__val">{pendingClaims}</div>
            <div className="dash-stat__label">Pending Claims</div>
          </div>
        </div>
        <div className="dash-stat dash-stat--green">
          <CheckCircle size={22} />
          <div>
            <div className="dash-stat__val">{approvedClaims}</div>
            <div className="dash-stat__label">Approved Claims</div>
          </div>
        </div>
      </div>

      <div className="dashboard-grid">
        {/* Left column */}
        <div>
          {/* Matching Found Items */}
          {matches.length > 0 && (
            <div className="dashboard-section" style={{ marginBottom: '1.5rem' }}>
              <div className="dashboard-section__header">
                <h2 className="dashboard-section__title" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Sparkles size={18} /> Possible Matching Found Items
                </h2>
                <Link to="/items" className="dashboard-section__link">Browse all <ArrowRight size={14} /></Link>
              </div>
              <div className="items-grid items-grid--sm">
                {matches.slice(0, 4).map(item => <ItemCard key={item.id} item={item} />)}
              </div>
            </div>
          )}

          {/* Recent Lost Reports */}
          <div className="dashboard-section">
            <div className="dashboard-section__header">
              <h2 className="dashboard-section__title">My Recent Lost Reports</h2>
              <Link to="/my-items" className="dashboard-section__link">View all <ArrowRight size={14} /></Link>
            </div>
            {items.length === 0
              ? <div className="empty-state">
                  <Package size={40} />
                  <p>No lost reports yet</p>
                  <Link to="/items/new" className="btn btn--primary btn--sm">Report your first lost item</Link>
                </div>
              : <div className="items-grid items-grid--sm">
                  {items.map(item => <ItemCard key={item.id} item={item} />)}
                </div>
            }
          </div>
        </div>

        {/* Right column */}
        <div className="dashboard-right">
          {/* Recent Claims */}
          <div className="dashboard-section">
            <div className="dashboard-section__header">
              <h2 className="dashboard-section__title">My Claim Requests</h2>
              <Link to="/my-claims" className="dashboard-section__link">View all <ArrowRight size={14} /></Link>
            </div>
            {claims.length === 0
              ? <div className="empty-state empty-state--sm"><FileText size={28} /><p>No claims submitted yet</p></div>
              : <div className="claims-list">
                  {claims.slice(0, 4).map(claim => {
                    const Icon = CLAIM_STATUS_ICON[claim.status]
                    return (
                      <Link key={claim.id} to="/my-claims" className="claim-row">
                        <div className={`claim-row__icon claim-row__icon--${CLAIM_STATUS_COLOR[claim.status]}`}>
                          <Icon size={16} />
                        </div>
                        <div className="claim-row__info">
                          <span className="claim-row__name">{claim.itemName}</span>
                          <span className={`claim-row__status claim-row__status--${CLAIM_STATUS_COLOR[claim.status]}`}>
                            {claim.status}
                          </span>
                        </div>
                      </Link>
                    )
                  })}
                </div>
            }
          </div>

          {/* Notifications */}
          <div className="dashboard-section">
            <div className="dashboard-section__header">
              <h2 className="dashboard-section__title">Notifications</h2>
              <Link to="/notifications" className="dashboard-section__link">View all <ArrowRight size={14} /></Link>
            </div>
            {notifications.length === 0
              ? <div className="empty-state empty-state--sm"><Bell size={28} /><p>No notifications</p></div>
              : <div className="notif-list">
                  {notifications.map(n => (
                    <div key={n.id} className={`notif-row ${!n.read ? 'notif-row--unread' : ''}`}>
                      <Bell size={14} />
                      <span>{n.message}</span>
                    </div>
                  ))}
                </div>
            }
          </div>
        </div>
      </div>
    </div>
  )
}
