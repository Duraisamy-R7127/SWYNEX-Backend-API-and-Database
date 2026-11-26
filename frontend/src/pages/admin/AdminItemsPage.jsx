import { useEffect, useState } from 'react'
import { adminApi } from '../../api/endpoints'
import Pagination from '../../components/ui/Pagination'
import { PageLoader } from '../../components/ui/Loaders'
import Modal from '../../components/ui/Modal'
import toast from 'react-hot-toast'
import { Link } from 'react-router-dom'
import { Package, RotateCcw, Edit3, Filter } from 'lucide-react'

const STATUS_COLOR = { OPEN: 'green', CLAIMED: 'yellow', RETURNED: 'blue' }
const TYPE_COLOR = { LOST: 'red', FOUND: 'teal' }

export default function AdminItemsPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [statusModal, setStatusModal] = useState(null)
  const [newStatus, setNewStatus] = useState('')
  const [filters, setFilters] = useState({ type: '', status: '', keyword: '' })

  const fetchItems = (p = 0, f = filters) => {
    setLoading(true)
    const params = { page: p, size: 15, sortBy: 'createdAt', direction: 'desc' }
    if (f.type) params.type = f.type
    if (f.status) params.status = f.status
    if (f.keyword) params.keyword = f.keyword
    adminApi.getItems(params)
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

  const handleStatusUpdate = async () => {
    try {
      if (newStatus === 'RETURNED') {
        await adminApi.markItemReturned(statusModal.id)
      } else {
        await adminApi.updateItemStatus(statusModal.id, newStatus)
      }
      toast.success('Item status updated')
      setStatusModal(null)
      fetchItems(page)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update status')
    }
  }

  if (loading) return <PageLoader />

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">All Items</h1>
          <p className="page__sub">{totalElements} items total</p>
        </div>
      </div>

      {/* Filters */}
      <div className="table-filters">
        <input className="form-input table-filters__search" placeholder="Search items…"
          value={filters.keyword} onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
          onKeyDown={(e) => e.key === 'Enter' && fetchItems(0)} />
        <select className="form-select" value={filters.type} onChange={(e) => { const f = { ...filters, type: e.target.value }; setFilters(f); fetchItems(0, f) }}>
          <option value="">All Types</option>
          <option value="LOST">Lost</option>
          <option value="FOUND">Found</option>
        </select>
        <select className="form-select" value={filters.status} onChange={(e) => { const f = { ...filters, status: e.target.value }; setFilters(f); fetchItems(0, f) }}>
          <option value="">All Statuses</option>
          <option value="OPEN">Open</option>
          <option value="CLAIMED">Claimed</option>
          <option value="RETURNED">Returned</option>
        </select>
        <button className="btn btn--primary" onClick={() => fetchItems(0)}>Apply</button>
      </div>

      <div className="table-card">
        <table className="data-table">
          <thead>
            <tr>
              <th>Item</th>
              <th>Category</th>
              <th>Type</th>
              <th>Status</th>
              <th>Location</th>
              <th>Posted By</th>
              <th>Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map(item => (
              <tr key={item.id}>
                <td>
                  <Link to={`/items/${item.id}`} className="table-link">{item.itemName}</Link>
                </td>
                <td>{item.category}</td>
                <td><span className={`status-badge status-badge--${TYPE_COLOR[item.itemType]}`}>{item.itemType}</span></td>
                <td><span className={`status-badge status-badge--${STATUS_COLOR[item.status]}`}>{item.status}</span></td>
                <td>{item.location}</td>
                <td>{item.postedByName}</td>
                <td className="table-date">{new Date(item.createdAt).toLocaleDateString()}</td>
                <td>
                  <button className="btn btn--ghost btn--sm" onClick={() => { setStatusModal(item); setNewStatus(item.status) }}>
                    <Edit3 size={14} /> Status
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Pagination page={page} totalPages={totalPages} onPageChange={(p) => fetchItems(p)} />

      <Modal isOpen={!!statusModal} onClose={() => setStatusModal(null)} title="Update Item Status">
        <p className="modal__desc">Change status for: <strong>{statusModal?.itemName}</strong></p>
        <div className="form-group">
          <label className="form-label">New Status</label>
          <select className="form-select" value={newStatus} onChange={(e) => setNewStatus(e.target.value)}>
            <option value="OPEN">Open</option>
            <option value="CLAIMED">Claimed</option>
            <option value="RETURNED">Returned</option>
          </select>
        </div>
        <div className="modal__footer">
          <button className="btn btn--ghost" onClick={() => setStatusModal(null)}>Cancel</button>
          <button className="btn btn--primary" onClick={handleStatusUpdate}>Update Status</button>
        </div>
      </Modal>
    </div>
  )
}
