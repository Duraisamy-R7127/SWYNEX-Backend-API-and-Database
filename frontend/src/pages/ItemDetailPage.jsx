import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { itemApi, claimApi, adminApi } from '../api/endpoints'
import { useAuth } from '../context/AuthContext'
import Modal from '../components/ui/Modal'
import { PageLoader } from '../components/ui/Loaders'
import toast from 'react-hot-toast'
import { MapPin, Calendar, User, Tag, ArrowLeft, Edit, Trash2, FileText, AlertCircle, Info, RotateCcw } from 'lucide-react'

const STATUS_COLOR = { OPEN: 'green', CLAIMED: 'yellow', RETURNED: 'blue' }
const STATUS_LABEL = { OPEN: 'Available', CLAIMED: 'Claim Approved (Awaiting Return)', RETURNED: 'Returned' }
const TYPE_COLOR = { LOST: 'red', FOUND: 'teal' }
const TYPE_LABEL = { LOST: 'Lost by Student', FOUND: 'Found by College' }

export default function ItemDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [item, setItem] = useState(null)
  const [loading, setLoading] = useState(true)
  const [claimModal, setClaimModal] = useState(false)
  const [deleteModal, setDeleteModal] = useState(false)
  const [proof, setProof] = useState('')
  const [additionalInfo, setAdditionalInfo] = useState('')
  const [proofError, setProofError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    itemApi.getItem(id)
      .then(r => setItem(r.data.data))
      .catch(() => navigate('/items'))
      .finally(() => setLoading(false))
  }, [id, navigate])

  const handleClaim = async () => {
    if (proof.length < 10) { setProofError('Proof must be at least 10 characters'); return }
    setProofError('')
    setSubmitting(true)
    try {
      const payload = {
        itemId: Number(id),
        proofDescription: proof,
        additionalDetails: additionalInfo || undefined
      }
      await claimApi.submitClaim(payload)
      toast.success('Claim submitted successfully! The admin will review your request.')
      setClaimModal(false)
      setProof('')
      setAdditionalInfo('')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit claim')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async () => {
    try {
      await itemApi.deleteItem(id)
      toast.success('Item deleted')
      navigate(isAdmin ? '/admin/found-items' : '/my-items')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to delete item')
    }
  }

  const handleMarkReturned = async () => {
    try {
      const r = await adminApi.markItemReturned(id)
      setItem(r.data.data)
      toast.success('Item marked as RETURNED')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to mark item as returned')
    }
  }

  if (loading) return <PageLoader />
  if (!item) return null

  const isAdmin = user?.role === 'ADMIN'
  const isOwner = user && item.postedById === user.id
  const isFoundItem = item.itemType === 'FOUND'
  // Students can claim FOUND items that are OPEN (and are not the item owner)
  const canClaim = user && !isAdmin && !isOwner && isFoundItem && item.status === 'OPEN'
  const imgSrc = item.imageUrl ? `http://localhost:8080${item.imageUrl}` : null

  return (
    <div className="page">
      <div className="page__back">
        <button className="btn btn--ghost btn--sm" onClick={() => navigate(-1)}>
          <ArrowLeft size={16} /> Back
        </button>
      </div>

      <div className="item-detail">
        {/* Image */}
        <div className="item-detail__img-wrap">
          {imgSrc
            ? <img src={imgSrc} alt={item.itemName} className="item-detail__img" />
            : <div className="item-detail__img-placeholder"><Tag size={64} /></div>
          }
          <span className={`item-detail__badge item-detail__badge--${TYPE_COLOR[item.itemType]}`}>
            {TYPE_LABEL[item.itemType]}
          </span>
        </div>

        {/* Info */}
        <div className="item-detail__info">
          <div className="item-detail__top">
            <h1 className="item-detail__title">{item.itemName}</h1>
            <span className={`status-badge status-badge--${STATUS_COLOR[item.status]}`}>
              {STATUS_LABEL[item.status] || item.status}
            </span>
          </div>

          <p className="item-detail__category">{item.category}</p>
          <p className="item-detail__desc">{item.description}</p>

          {item.additionalDetails && (
            <div className="item-detail__additional">
              <h3 className="item-detail__section-label">Additional Details</h3>
              <p>{item.additionalDetails}</p>
            </div>
          )}

          <div className="item-detail__meta">
            <div className="item-detail__meta-row">
              <MapPin size={16} />
              <span>{isFoundItem ? 'Found at: ' : 'Lost at: '}{item.location}</span>
            </div>
            <div className="item-detail__meta-row">
              <Calendar size={16} />
              <span>{isFoundItem ? 'Found on: ' : 'Lost on: '}{item.eventDate}</span>
            </div>
            <div className="item-detail__meta-row">
              <User size={16} />
              <span>{isFoundItem ? 'Registered by: College Administration' : `Reported by: ${item.postedByName}`}</span>
            </div>
            <div className="item-detail__meta-row">
              <Calendar size={16} />
              <span>Registered on: {new Date(item.createdAt).toLocaleDateString()}</span>
            </div>
          </div>

          {/* Claim instructions for FOUND items */}
          {isFoundItem && item.status === 'OPEN' && !user && (
            <div className="info-banner">
              <AlertCircle size={16} />
              <span><Link to="/login">Sign in</Link> as a student to submit a claim for this item.</span>
            </div>
          )}

          {isFoundItem && item.status !== 'OPEN' && (
            <div className="info-banner info-banner--grey">
              <Info size={16} />
              <span>
                {item.status === 'CLAIMED'
                  ? 'A claim for this item has been approved by the admin and is awaiting physical return.'
                  : 'This item has already been returned to its owner.'}
              </span>
            </div>
          )}

          <div className="item-detail__actions">
            {canClaim && (
              <button className="btn btn--primary" onClick={() => setClaimModal(true)}>
                <FileText size={16} /> Submit Ownership Claim
              </button>
            )}
            {/* Admin can edit/delete/mark returned on found items */}
            {isAdmin && isFoundItem && (
              <>
                {item.status === 'CLAIMED' && (
                  <button className="btn btn--success" onClick={handleMarkReturned}>
                    <RotateCcw size={16} /> Mark as Returned
                  </button>
                )}
                {item.status !== 'RETURNED' && (
                  <Link to={`/items/${id}/edit`} className="btn btn--outline">
                    <Edit size={16} /> Edit
                  </Link>
                )}
                <button className="btn btn--danger" onClick={() => setDeleteModal(true)}>
                  <Trash2 size={16} /> Delete
                </button>
              </>
            )}
            {/* Student can edit/delete their own LOST reports only */}
            {!isAdmin && isOwner && !isFoundItem && (
              <>
                <Link to={`/items/${id}/edit`} className="btn btn--outline">
                  <Edit size={16} /> Edit Report
                </Link>
                <button className="btn btn--danger" onClick={() => setDeleteModal(true)}>
                  <Trash2 size={16} /> Delete Report
                </button>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Claim Modal */}
      <Modal isOpen={claimModal} onClose={() => setClaimModal(false)} title="Submit Ownership Claim">
        <div className="info-banner info-banner--blue" style={{ marginBottom: '1rem' }}>
          <Info size={16} />
          <span>Provide detailed proof of ownership. The college admin will verify your claim before approval.</span>
        </div>
        <div className="form-group">
          <label className="form-label">Proof of Ownership *</label>
          <textarea
            className={`form-textarea ${proofError ? 'form-input--error' : ''}`}
            rows={4}
            placeholder="Describe identifying features, contents, purchase details, serial number, etc. (min 10 characters)"
            value={proof}
            onChange={(e) => setProof(e.target.value)}
          />
          {proofError && <span className="form-error">{proofError}</span>}
          <span className="form-hint">{proof.length}/2000 characters</span>
        </div>
        <div className="form-group">
          <label className="form-label">Additional Identifying Details <span className="form-optional">(optional)</span></label>
          <textarea
            className="form-textarea"
            rows={2}
            placeholder="Any other details that help verify ownership…"
            value={additionalInfo}
            onChange={(e) => setAdditionalInfo(e.target.value)}
          />
        </div>
        <div className="modal__footer">
          <button className="btn btn--ghost" onClick={() => setClaimModal(false)}>Cancel</button>
          <button className="btn btn--primary" onClick={handleClaim} disabled={submitting}>
            {submitting ? 'Submitting…' : 'Submit Claim'}
          </button>
        </div>
      </Modal>

      {/* Delete Modal */}
      <Modal isOpen={deleteModal} onClose={() => setDeleteModal(false)} title="Delete Item">
        <p className="modal__desc">
          Are you sure you want to delete <strong>{item.itemName}</strong>? This action cannot be undone.
        </p>
        <div className="modal__footer">
          <button className="btn btn--ghost" onClick={() => setDeleteModal(false)}>Cancel</button>
          <button className="btn btn--danger" onClick={handleDelete}>Delete</button>
        </div>
      </Modal>
    </div>
  )
}
