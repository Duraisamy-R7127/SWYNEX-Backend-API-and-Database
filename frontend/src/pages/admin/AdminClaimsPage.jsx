import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { adminApi } from '../../api/endpoints'
import Pagination from '../../components/ui/Pagination'
import { PageLoader } from '../../components/ui/Loaders'
import Modal from '../../components/ui/Modal'
import toast from 'react-hot-toast'
import { CheckCircle, XCircle, Clock, Eye } from 'lucide-react'

const STATUS_COLOR = { PENDING: 'yellow', APPROVED: 'green', REJECTED: 'red' }
const STATUS_ICON = { PENDING: Clock, APPROVED: CheckCircle, REJECTED: XCircle }

export default function AdminClaimsPage() {
  const [searchParams] = useSearchParams()
  const [claims, setClaims] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [statusFilter, setStatusFilter] = useState(searchParams.get('status') || '')
  const [verifyModal, setVerifyModal] = useState(null)
  const [decision, setDecision] = useState({ status: 'APPROVED', adminRemarks: '' })
  const [submitting, setSubmitting] = useState(false)

  const fetchClaims = (p = 0, sf = statusFilter) => {
    setLoading(true)
    const params = { page: p, size: 15 }
    if (sf) params.status = sf
    adminApi.getClaims(params)
      .then(r => {
        setClaims(r.data.data.content)
        setTotalPages(r.data.data.totalPages)
        setTotalElements(r.data.data.totalElements)
        setPage(p)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchClaims() }, [])

  const handleVerify = async () => {
    if (decision.status === 'REJECTED' && !decision.adminRemarks.trim()) {
      toast.error('Remarks are required when rejecting a claim')
      return
    }
    setSubmitting(true)
    try {
      await adminApi.verifyClaim(verifyModal.id, decision)
      toast.success(`Claim ${decision.status.toLowerCase()} successfully`)
      setVerifyModal(null)
      fetchClaims(page)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to verify claim')
    } finally {
      setSubmitting(false)
    }
  }

  const handleMarkReturned = async (claim) => {
    try {
      await adminApi.markItemReturned(claim.itemId)
      toast.success(`'${claim.itemName}' marked as RETURNED`)
      fetchClaims(page)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to mark item as returned')
    }
  }

  if (loading) return <PageLoader />

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">All Claims</h1>
          <p className="page__sub">{totalElements} claims total</p>
        </div>
      </div>

      {/* Status filter tabs */}
      <div className="tab-filters">
        {['', 'PENDING', 'APPROVED', 'REJECTED'].map(s => (
          <button key={s}
            className={`tab-filter ${statusFilter === s ? 'tab-filter--active' : ''}`}
            onClick={() => { setStatusFilter(s); fetchClaims(0, s) }}>
            {s || 'All'}
          </button>
        ))}
      </div>

      <div className="table-card">
        <table className="data-table">
          <thead>
            <tr>
              <th>Item</th>
              <th>Claimant</th>
              <th>Proof (preview)</th>
              <th>Status</th>
              <th>Submitted</th>
              <th>Reviewed</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {claims.map(claim => {
              const Icon = STATUS_ICON[claim.status]
              return (
                <tr key={claim.id}>
                  <td><Link to={`/items/${claim.itemId}`} className="table-link">{claim.itemName}</Link></td>
                  <td>{claim.claimantName}</td>
                  <td className="table-proof">{claim.proofDescription.slice(0, 60)}…</td>
                  <td>
                    <span className={`status-badge status-badge--${STATUS_COLOR[claim.status]}`}>
                      <Icon size={12} /> {claim.status}
                    </span>
                  </td>
                  <td className="table-date">{new Date(claim.createdAt).toLocaleDateString()}</td>
                  <td className="table-date">{claim.reviewedAt ? new Date(claim.reviewedAt).toLocaleDateString() : <span className="text-muted">—</span>}</td>
                  <td>
                    {claim.status === 'PENDING' && (
                      <button className="btn btn--primary btn--sm"
                        onClick={() => { setVerifyModal(claim); setDecision({ status: 'APPROVED', adminRemarks: '' }) }}>
                        <Eye size={14} /> Review
                      </button>
                    )}
                    {claim.status === 'APPROVED' && claim.itemStatus === 'CLAIMED' && (
                      <button className="btn btn--success btn--sm" onClick={() => handleMarkReturned(claim)}>
                        Mark Returned
                      </button>
                    )}
                    {(claim.status === 'REJECTED' || (claim.status === 'APPROVED' && claim.itemStatus === 'RETURNED')) && (
                      <span className="text-muted text-sm">{claim.adminRemarks || claim.itemStatus}</span>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {claims.length === 0 && (
          <div className="empty-state"><Clock size={36} /><p>No claims found</p></div>
        )}
      </div>

      <Pagination page={page} totalPages={totalPages} onPageChange={(p) => fetchClaims(p)} />

      {/* Verify Modal */}
      <Modal isOpen={!!verifyModal} onClose={() => setVerifyModal(null)} title="Review Claim" size="lg">
        {verifyModal && (
          <>
            <div className="claim-review-info">
              <div className="claim-review-row">
                <span className="claim-review-label">Item:</span>
                <Link to={`/items/${verifyModal.itemId}`} className="table-link">{verifyModal.itemName}</Link>
              </div>
              <div className="claim-review-row">
                <span className="claim-review-label">Claimant:</span>
                <span>{verifyModal.claimantName}{verifyModal.claimantEmail ? ` (${verifyModal.claimantEmail})` : ''}{verifyModal.claimantPhone ? ` • ${verifyModal.claimantPhone}` : ''}</span>
              </div>
              <div className="claim-review-row">
                <span className="claim-review-label">Submitted:</span>
                <span>{new Date(verifyModal.createdAt).toLocaleString()}</span>
              </div>
              <div className="claim-review-proof">
                <span className="claim-review-label">Proof of Ownership:</span>
                <p className="claim-review-proof__text">{verifyModal.proofDescription}</p>
              </div>
              {verifyModal.additionalDetails && (
                <div className="claim-review-proof">
                  <span className="claim-review-label">Additional Identifying Details:</span>
                  <p className="claim-review-proof__text">{verifyModal.additionalDetails}</p>
                </div>
              )}
            </div>

            <div className="form-group">
              <label className="form-label">Decision</label>
              <div className="decision-toggle">
                {['APPROVED', 'REJECTED'].map(s => (
                  <button key={s} type="button"
                    className={`decision-btn decision-btn--${s.toLowerCase()} ${decision.status === s ? 'decision-btn--active' : ''}`}
                    onClick={() => setDecision({ ...decision, status: s })}>
                    {s === 'APPROVED' ? <CheckCircle size={16} /> : <XCircle size={16} />} {s}
                  </button>
                ))}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">
                Admin Remarks {decision.status === 'REJECTED' && <span className="form-required">*</span>}
              </label>
              <textarea className="form-textarea" rows={3}
                placeholder={decision.status === 'REJECTED' ? 'Required: explain why the claim is rejected' : 'Optional remarks for the claimant'}
                value={decision.adminRemarks}
                onChange={(e) => setDecision({ ...decision, adminRemarks: e.target.value })} />
            </div>

            <div className="modal__footer">
              <button className="btn btn--ghost" onClick={() => setVerifyModal(null)}>Cancel</button>
              <button
                className={`btn ${decision.status === 'APPROVED' ? 'btn--success' : 'btn--danger'}`}
                onClick={handleVerify} disabled={submitting}>
                {submitting ? 'Processing…' : `${decision.status === 'APPROVED' ? 'Approve' : 'Reject'} Claim`}
              </button>
            </div>
          </>
        )}
      </Modal>
    </div>
  )
}
