import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { userApi } from '../api/endpoints'
import { PageLoader } from '../components/ui/Loaders'
import { FileText, Clock, CheckCircle, XCircle, Eye } from 'lucide-react'

const STATUS_CONFIG = {
  PENDING:  { icon: Clock,         color: 'yellow', label: 'Pending Review' },
  APPROVED: { icon: CheckCircle,   color: 'green',  label: 'Approved' },
  REJECTED: { icon: XCircle,       color: 'red',    label: 'Rejected' },
}

export default function MyClaimsPage() {
  const [claims, setClaims] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    userApi.getMyClaims()
      .then(r => setClaims(r.data.data || []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <PageLoader />

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">My Claims</h1>
          <p className="page__sub">{claims.length} claim{claims.length !== 1 ? 's' : ''} submitted</p>
        </div>
      </div>

      {claims.length === 0
        ? <div className="empty-state empty-state--lg">
            <FileText size={56} />
            <h3>No claims yet</h3>
            <p>Browse items and submit a claim when you find something that belongs to you.</p>
            <Link to="/items" className="btn btn--primary">Browse Items</Link>
          </div>
        : <div className="claims-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Item</th>
                  <th>Item Status</th>
                  <th>Claim Status</th>
                  <th>Submitted</th>
                  <th>Admin Remarks</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {claims.map(claim => {
                  const cfg = STATUS_CONFIG[claim.status]
                  const Icon = cfg.icon
                  return (
                    <tr key={claim.id}>
                      <td>
                        <Link to={`/items/${claim.itemId}`} className="table-link">{claim.itemName}</Link>
                      </td>
                      <td>
                        <span className={`status-badge status-badge--${claim.itemStatus === 'OPEN' ? 'green' : claim.itemStatus === 'CLAIMED' ? 'yellow' : 'blue'}`}>
                          {claim.itemStatus}
                        </span>
                      </td>
                      <td>
                        <span className={`status-badge status-badge--${cfg.color}`}>
                          <Icon size={12} /> {cfg.label}
                        </span>
                      </td>
                      <td className="table-date">{new Date(claim.createdAt).toLocaleDateString()}</td>
                      <td className="table-remarks">{claim.adminRemarks || <span className="text-muted">—</span>}</td>
                      <td>
                        <Link to={`/items/${claim.itemId}`} className="btn btn--ghost btn--sm">
                          <Eye size={14} /> View Item
                        </Link>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
      }
    </div>
  )
}
