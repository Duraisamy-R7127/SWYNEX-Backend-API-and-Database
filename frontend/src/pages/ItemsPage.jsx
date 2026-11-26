import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { itemApi } from '../api/endpoints'
import { useAuth } from '../context/AuthContext'
import ItemCard from '../components/ui/ItemCard'
import Pagination from '../components/ui/Pagination'
import { SkeletonCard } from '../components/ui/Loaders'
import { Search, Filter, X, AlertCircle } from 'lucide-react'

const CATEGORIES = [
  'Mobile Phones', 'Wallets', 'ID Cards', 'Bags', 'Books',
  'Watches', 'Keys', 'Earphones', 'Electronic Devices', 'Clothing', 'Other Items'
]

export default function ItemsPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [showFilters, setShowFilters] = useState(false)
  // For students, default to FOUND items only. Admin can see all.
  const [filters, setFilters] = useState({ keyword: '', category: '', location: '', eventDate: '', status: '' })
  const [applied, setApplied] = useState({})

  const fetchItems = useCallback((p = 0, f = applied) => {
    setLoading(true)
    const params = { page: p, size: 12, sortBy: 'createdAt', direction: 'desc', type: 'FOUND' }
    if (f.keyword) params.keyword = f.keyword
    if (f.category) params.category = f.category
    if (f.location) params.location = f.location
    if (f.eventDate) params.eventDate = f.eventDate
    if (f.status) params.status = f.status
    itemApi.getItems(params)
      .then(r => {
        setItems(r.data.data.content)
        setTotalPages(r.data.data.totalPages)
        setTotalElements(r.data.data.totalElements)
        setPage(p)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [applied])

  useEffect(() => { fetchItems(0, {}) }, [])

  const applyFilters = () => {
    setApplied({ ...filters })
    fetchItems(0, filters)
    setShowFilters(false)
  }

  const clearFilters = () => {
    const empty = { keyword: '', category: '', location: '', eventDate: '', status: '' }
    setFilters(empty)
    setApplied(empty)
    fetchItems(0, empty)
  }

  const hasFilters = Object.values(applied).some(Boolean)

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">Official Found Items</h1>
          <p className="page__sub">
            {totalElements} item{totalElements !== 1 ? 's' : ''} registered by the college administration
          </p>
        </div>
        {user && !isAdmin && (
          <Link to="/items/new" className="btn btn--outline">
            <AlertCircle size={16} /> Report Lost Item
          </Link>
        )}
      </div>

      {/* Search bar */}
      <div className="search-bar">
        <Search size={18} className="search-bar__icon" />
        <input
          className="search-bar__input"
          placeholder="Search found items by name, description, or location…"
          value={filters.keyword}
          onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
          onKeyDown={(e) => e.key === 'Enter' && applyFilters()}
        />
        {filters.keyword && (
          <button className="search-bar__clear" onClick={() => setFilters({ ...filters, keyword: '' })}>
            <X size={16} />
          </button>
        )}
        <button className={`btn ${showFilters ? 'btn--primary' : 'btn--outline'} search-bar__btn`}
          onClick={() => setShowFilters(!showFilters)}>
          <Filter size={16} /> Filter
        </button>
        <button className="btn btn--primary search-bar__btn" onClick={applyFilters}>Search</button>
      </div>

      {/* Filter panel */}
      {showFilters && (
        <div className="filter-panel">
          <div className="filter-panel__grid">
            <div className="form-group">
              <label className="form-label">Category</label>
              <select className="form-select" value={filters.category}
                onChange={(e) => setFilters({ ...filters, category: e.target.value })}>
                <option value="">All Categories</option>
                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Location Found</label>
              <input className="form-input" placeholder="e.g. Library" value={filters.location}
                onChange={(e) => setFilters({ ...filters, location: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">Found Date</label>
              <input type="date" className="form-input"
                max={new Date().toISOString().split('T')[0]}
                value={filters.eventDate}
                onChange={(e) => setFilters({ ...filters, eventDate: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">Status</label>
              <select className="form-select" value={filters.status}
                onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
                <option value="">All Statuses</option>
                <option value="OPEN">Available</option>
                <option value="CLAIMED">Claim Approved</option>
                <option value="RETURNED">Returned</option>
              </select>
            </div>
          </div>
          <div className="filter-panel__actions">
            <button className="btn btn--ghost" onClick={clearFilters}>Clear All</button>
            <button className="btn btn--primary" onClick={applyFilters}>Apply Filters</button>
          </div>
        </div>
      )}

      {/* Active filter chips */}
      {hasFilters && (
        <div className="filter-chips">
          {Object.entries(applied).filter(([, v]) => v).map(([k, v]) => (
            <span key={k} className="chip">
              {k}: {v}
              <button onClick={() => {
                const f = { ...applied, [k]: '' }
                setApplied(f); setFilters(f); fetchItems(0, f)
              }}><X size={12} /></button>
            </span>
          ))}
        </div>
      )}

      {/* Grid */}
      {loading
        ? <div className="items-grid">{Array(8).fill(0).map((_, i) => <SkeletonCard key={i} />)}</div>
        : items.length === 0
          ? <div className="empty-state empty-state--lg">
              <Search size={48} />
              <h3>No found items registered yet</h3>
              <p>
                {hasFilters
                  ? 'Try adjusting your search or filters.'
                  : 'The college admin has not registered any found items yet. Check back later.'}
              </p>
              {hasFilters && <button className="btn btn--outline" onClick={clearFilters}>Clear Filters</button>}
            </div>
          : <div className="items-grid">{items.map(item => <ItemCard key={item.id} item={item} />)}</div>
      }

      <Pagination page={page} totalPages={totalPages} onPageChange={(p) => fetchItems(p)} />
    </div>
  )
}
