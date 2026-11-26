import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { userApi, itemApi } from '../api/endpoints'
import ItemCard from '../components/ui/ItemCard'
import Pagination from '../components/ui/Pagination'
import { SkeletonCard } from '../components/ui/Loaders'
import Modal from '../components/ui/Modal'
import toast from 'react-hot-toast'
import { AlertCircle, Package, Trash2, Search } from 'lucide-react'

export default function MyItemsPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [deleteTarget, setDeleteTarget] = useState(null)

  const fetchItems = (p = 0) => {
    setLoading(true)
    userApi.getMyItems(p, 12)
      .then(r => {
        setItems(r.data.data.content)
        setTotalPages(r.data.data.totalPages)
        setPage(p)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchItems() }, [])

  const handleDelete = async () => {
    try {
      await itemApi.deleteItem(deleteTarget.id)
      toast.success('Lost report deleted')
      setDeleteTarget(null)
      fetchItems(page)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to delete')
    }
  }

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">My Lost Reports</h1>
          <p className="page__sub">Items you have reported as lost</p>
        </div>
        <div className="header-actions">
          <Link to="/items" className="btn btn--outline">
            <Search size={16} /> Search Found Items
          </Link>
          <Link to="/items/new" className="btn btn--primary">
            <AlertCircle size={16} /> Report New Lost Item
          </Link>
        </div>
      </div>

      {loading
        ? <div className="items-grid">{Array(6).fill(0).map((_, i) => <SkeletonCard key={i} />)}</div>
        : items.length === 0
          ? <div className="empty-state empty-state--lg">
              <Package size={56} />
              <h3>No lost reports yet</h3>
              <p>Report your lost items so the admin can help match them with found items.</p>
              <Link to="/items/new" className="btn btn--primary">Report a Lost Item</Link>
            </div>
          : <>
              <div className="items-grid">
                {items.map(item => (
                  <div key={item.id} className="item-card-wrap">
                    <ItemCard item={item} />
                    <div className="item-card-wrap__actions">
                      <Link to={`/items/${item.id}/edit`} className="btn btn--outline btn--sm">Edit</Link>
                      <button className="btn btn--danger btn--sm" onClick={() => setDeleteTarget(item)}>
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
              <Pagination page={page} totalPages={totalPages} onPageChange={fetchItems} />
            </>
      }

      <Modal isOpen={!!deleteTarget} onClose={() => setDeleteTarget(null)} title="Delete Lost Report">
        <p className="modal__desc">
          Delete lost report for <strong>{deleteTarget?.itemName}</strong>? This cannot be undone.
        </p>
        <div className="modal__footer">
          <button className="btn btn--ghost" onClick={() => setDeleteTarget(null)}>Cancel</button>
          <button className="btn btn--danger" onClick={handleDelete}>Delete</button>
        </div>
      </Modal>
    </div>
  )
}
