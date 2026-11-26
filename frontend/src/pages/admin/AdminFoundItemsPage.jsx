import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../../api/endpoints'
import Pagination from '../../components/ui/Pagination'
import { SkeletonCard } from '../../components/ui/Loaders'
import Modal from '../../components/ui/Modal'
import toast from 'react-hot-toast'
import { PlusCircle, Edit, Trash2, Package, Upload, X, Eye } from 'lucide-react'

const CATEGORIES = [
  'Mobile Phones', 'Wallets', 'ID Cards', 'Bags', 'Books',
  'Watches', 'Keys', 'Earphones', 'Electronic Devices', 'Clothing', 'Other Items'
]

const STATUS_COLOR = { OPEN: 'green', CLAIMED: 'yellow', RETURNED: 'blue' }
const STATUS_LABEL = { OPEN: 'Available', CLAIMED: 'Claim Approved', RETURNED: 'Returned' }

const emptyForm = {
  itemName: '', category: '', description: '', location: '',
  eventDate: '', additionalDetails: ''
}

export default function AdminFoundItemsPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [image, setImage] = useState(null)
  const [preview, setPreview] = useState(null)
  const [existingImg, setExistingImg] = useState(null)
  const [removeImage, setRemoveImage] = useState(false)
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [keyword, setKeyword] = useState('')

  const fetchItems = (p = 0, kw = keyword) => {
    setLoading(true)
    const params = { page: p, size: 12 }
    if (kw) params.keyword = kw
    adminApi.getFoundItems(params)
      .then(r => {
        setItems(r.data.data.content)
        setTotalPages(r.data.data.totalPages)
        setTotalElements(r.data.data.totalElements)
        setPage(p)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchItems() }, [])

  const openAdd = () => {
    setEditTarget(null)
    setForm(emptyForm)
    setImage(null)
    setPreview(null)
    setExistingImg(null)
    setRemoveImage(false)
    setErrors({})
    setShowForm(true)
  }

  const openEdit = (item) => {
    setEditTarget(item)
    setForm({
      itemName: item.itemName,
      category: item.category,
      description: item.description,
      location: item.location,
      eventDate: item.eventDate,
      additionalDetails: item.additionalDetails || '',
    })
    setImage(null)
    setPreview(null)
    setExistingImg(item.imageUrl ? `http://localhost:8080${item.imageUrl}` : null)
    setRemoveImage(false)
    setErrors({})
    setShowForm(true)
  }

  const validate = () => {
    const e = {}
    if (!form.itemName.trim()) e.itemName = 'Item name is required'
    if (!form.category) e.category = 'Category is required'
    if (!form.description.trim() || form.description.length < 5) e.description = 'Description required (min 5 chars)'
    if (!form.location.trim()) e.location = 'Location is required'
    if (!form.eventDate) e.eventDate = 'Found date is required'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleImageChange = (e) => {
    const file = e.target.files[0]
    if (!file) return
    if (file.size > 5 * 1024 * 1024) { toast.error('Image must be under 5MB'); return }
    setImage(file)
    setPreview(URL.createObjectURL(file))
    setRemoveImage(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return
    setSubmitting(true)
    try {
      const fd = new FormData()
      fd.append('itemName', form.itemName)
      fd.append('category', form.category)
      fd.append('description', form.description)
      fd.append('location', form.location)
      fd.append('eventDate', form.eventDate)
      fd.append('itemType', 'FOUND')
      if (form.additionalDetails) fd.append('additionalDetails', form.additionalDetails)
      if (image) fd.append('image', image)
      if (editTarget) fd.append('removeImage', removeImage)

      if (editTarget) {
        await adminApi.updateFoundItem(editTarget.id, fd)
        toast.success('Found item updated successfully')
      } else {
        await adminApi.addFoundItem(fd)
        toast.success('Found item added successfully')
      }
      setShowForm(false)
      fetchItems(page)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save item')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async () => {
    try {
      await adminApi.deleteFoundItem(deleteTarget.id)
      toast.success('Found item deleted')
      setDeleteTarget(null)
      fetchItems(page)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to delete')
    }
  }

  const handleMarkReturned = async (item) => {
    try {
      await adminApi.markItemReturned(item.id)
      toast.success(`${item.itemName} marked as RETURNED`)
      fetchItems(page)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update status')
    }
  }

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">Manage Found Items</h1>
          <p className="page__sub">{totalElements} found items registered</p>
        </div>
        <button className="btn btn--primary" onClick={openAdd}>
          <PlusCircle size={16} /> Add Found Item
        </button>
      </div>

      {/* Search */}
      <div className="search-bar" style={{ marginBottom: '1.5rem' }}>
        <input className="search-bar__input" placeholder="Search found items…"
          value={keyword} onChange={e => setKeyword(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && fetchItems(0, keyword)} />
        <button className="btn btn--primary search-bar__btn" onClick={() => fetchItems(0, keyword)}>Search</button>
      </div>

      {/* Table */}
      {loading ? (
        <div className="items-grid">{Array(6).fill(0).map((_, i) => <SkeletonCard key={i} />)}</div>
      ) : items.length === 0 ? (
        <div className="empty-state empty-state--lg">
          <Package size={56} />
          <h3>No found items yet</h3>
          <p>Add a found item to allow students to claim it.</p>
          <button className="btn btn--primary" onClick={openAdd}>Add First Found Item</button>
        </div>
      ) : (
        <div className="table-card">
          <table className="data-table">
            <thead>
              <tr>
                <th>Item</th>
                <th>Category</th>
                <th>Found Location</th>
                <th>Found Date</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => (
                <tr key={item.id}>
                  <td>
                    <div className="table-item-cell">
                      {item.imageUrl && (
                        <img src={`http://localhost:8080${item.imageUrl}`} alt={item.itemName}
                          className="table-item-thumb" />
                      )}
                      <div>
                        <div className="table-item-name">{item.itemName}</div>
                        <div className="table-item-desc text-muted text-sm">{item.description.slice(0, 60)}…</div>
                      </div>
                    </div>
                  </td>
                  <td>{item.category}</td>
                  <td>{item.location}</td>
                  <td className="table-date">{item.eventDate}</td>
                  <td>
                    <span className={`status-badge status-badge--${STATUS_COLOR[item.status]}`}>
                      {STATUS_LABEL[item.status] || item.status}
                    </span>
                  </td>
                  <td>
                    <div className="table-actions">
                      <Link to={`/items/${item.id}`} className="btn btn--ghost btn--sm" title="View">
                        <Eye size={14} />
                      </Link>
                      {item.status !== 'RETURNED' && (
                        <button className="btn btn--outline btn--sm" title="Edit" onClick={() => openEdit(item)}>
                          <Edit size={14} />
                        </button>
                      )}
                      {item.status === 'CLAIMED' && (
                        <button className="btn btn--success btn--sm" onClick={() => handleMarkReturned(item)}>
                          Mark Returned
                        </button>
                      )}
                      <button className="btn btn--danger btn--sm" title="Delete" onClick={() => setDeleteTarget(item)}>
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onPageChange={p => fetchItems(p)} />

      {/* Add/Edit Modal */}
      <Modal isOpen={showForm} onClose={() => setShowForm(false)}
        title={editTarget ? 'Edit Found Item' : 'Add Found Item'} size="lg">
        <form onSubmit={handleSubmit} noValidate>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Item Name *</label>
              <input className={`form-input ${errors.itemName ? 'form-input--error' : ''}`}
                placeholder="e.g. Black Samsung Galaxy S23" value={form.itemName} onChange={set('itemName')} />
              {errors.itemName && <span className="form-error">{errors.itemName}</span>}
            </div>
            <div className="form-group">
              <label className="form-label">Category *</label>
              <select className={`form-select ${errors.category ? 'form-input--error' : ''}`}
                value={form.category} onChange={set('category')}>
                <option value="">Select category</option>
                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
              {errors.category && <span className="form-error">{errors.category}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Description *</label>
            <textarea className={`form-textarea ${errors.description ? 'form-input--error' : ''}`}
              rows={3} placeholder="Describe the item in detail…"
              value={form.description} onChange={set('description')} />
            {errors.description && <span className="form-error">{errors.description}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Additional Identifying Details <span className="form-optional">(optional)</span></label>
            <textarea className="form-textarea" rows={2}
              placeholder="Any unique marks, serial number, or other details…"
              value={form.additionalDetails} onChange={set('additionalDetails')} />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Found Location *</label>
              <input className={`form-input ${errors.location ? 'form-input--error' : ''}`}
                placeholder="e.g. Main Library, 2nd Floor" value={form.location} onChange={set('location')} />
              {errors.location && <span className="form-error">{errors.location}</span>}
            </div>
            <div className="form-group">
              <label className="form-label">Date Found *</label>
              <input type="date" className={`form-input ${errors.eventDate ? 'form-input--error' : ''}`}
                max={new Date().toISOString().split('T')[0]}
                value={form.eventDate} onChange={set('eventDate')} />
              {errors.eventDate && <span className="form-error">{errors.eventDate}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Photo <span className="form-optional">(optional, max 5MB)</span></label>
            {(preview || (existingImg && !removeImage)) ? (
              <div className="img-preview">
                <img src={preview || existingImg} alt="preview" />
                <button type="button" className="img-preview__remove"
                  onClick={() => { setImage(null); setPreview(null); if (editTarget) setRemoveImage(true) }}>
                  <X size={16} />
                </button>
              </div>
            ) : (
              <label className="upload-zone">
                <Upload size={28} />
                <span>Click to upload or drag &amp; drop</span>
                <span className="upload-zone__hint">PNG, JPG, WEBP up to 5MB</span>
                <input type="file" accept="image/*" className="upload-zone__input" onChange={handleImageChange} />
              </label>
            )}
          </div>

          <div className="modal__footer">
            <button type="button" className="btn btn--ghost" onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? 'Saving…' : editTarget ? 'Update Item' : 'Add Found Item'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Delete Modal */}
      <Modal isOpen={!!deleteTarget} onClose={() => setDeleteTarget(null)} title="Delete Found Item">
        <p className="modal__desc">
          Are you sure you want to delete <strong>{deleteTarget?.itemName}</strong>?
          Any pending claims will be cancelled. This cannot be undone.
        </p>
        <div className="modal__footer">
          <button className="btn btn--ghost" onClick={() => setDeleteTarget(null)}>Cancel</button>
          <button className="btn btn--danger" onClick={handleDelete}>Delete Item</button>
        </div>
      </Modal>
    </div>
  )
}
