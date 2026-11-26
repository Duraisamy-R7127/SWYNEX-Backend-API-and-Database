import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../../api/endpoints'
import Pagination from '../../components/ui/Pagination'
import { SkeletonCard } from '../../components/ui/Loaders'
import { AlertCircle, Eye, Search } from 'lucide-react'

const CATEGORIES = [
  'Mobile Phones', 'Wallets', 'ID Cards', 'Bags', 'Books',
  'Watches', 'Keys', 'Earphones', 'Electronic Devices', 'Clothing', 'Other Items'
]

const STATUS_COLOR = { OPEN: 'red', CLAIMED: 'yellow', RETURNED: 'blue' }

export default function AdminLostReportsPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')

  const fetchReports = (p = 0, kw = keyword, cat = category) => {
    setLoading(true)
    const params = { page: p, size: 15 }
    if (kw) params.keyword = kw
    if (cat) params.category = cat
    adminApi.getLostReports(params)
      .then(r => {
        setItems(r.data.data.content)
        setTotalPages(r.data.data.totalPages)
        setTotalElements(r.data.data.totalElements)
        setPage(p)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchReports() }, [])

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">Student Lost Reports</h1>
          <p className="page__sub">{totalElements} lost item report{totalElements !== 1 ? 's' : ''} submitted by students</p>
        </div>
      </div>

      <div className="search-bar" style={{ marginBottom: '1.5rem' }}>
        <Search size={18} className="search-bar__icon" />
        <input
          className="search-bar__input"
          placeholder="Search lost reports by item name or description…"
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && fetchReports(0, keyword, category)}
        />
        <select
          className="form-select"
          style={{ maxWidth: '200px' }}
          value={category}
          onChange={e => { setCategory(e.target.value); fetchReports(0, keyword, e.target.value) }}
        >
          <option value="">All Categories</option>
          {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
        <button className="btn btn--primary search-bar__btn" onClick={() => fetchReports(0, keyword, category)}>
          Search
        </button>
      </div>

      {loading ? (
        <div className="items-grid">{Array(6).fill(0).map((_, i) => <SkeletonCard key={i} />)}</div>
      ) : items.length === 0 ? (
        <div className="empty-state empty-state--lg">
          <AlertCircle size={56} />
          <h3>No lost reports found</h3>
          <p>No students have reported lost items matching your criteria.</p>
        </div>
      ) : (
        <div className="table-card">
          <table className="data-table">
            <thead>
              <tr>
                <th>Item</th>
                <th>Student</th>
                <th>Category</th>
                <th>Lost Location</th>
                <th>Lost Date</th>
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
                        <img src={`http://localhost:8080${item.imageUrl}`} alt={item.itemName} className="table-item-thumb" />
                      )}
                      <div>
                        <div className="table-item-name">{item.itemName}</div>
                        <div className="table-item-desc text-muted text-sm">{item.description.slice(0, 60)}…</div>
                      </div>
                    </div>
                  </td>
                  <td>{item.postedByName}</td>
                  <td>{item.category}</td>
                  <td>{item.location}</td>
                  <td className="table-date">{item.eventDate}</td>
                  <td>
                    <span className={`status-badge status-badge--${STATUS_COLOR[item.status] || 'blue'}`}>
                      {item.status}
                    </span>
                  </td>
                  <td>
                    <Link to={`/items/${item.id}`} className="btn btn--ghost btn--sm" title="View Details">
                      <Eye size={14} /> View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onPageChange={p => fetchReports(p)} />
    </div>
  )
}
