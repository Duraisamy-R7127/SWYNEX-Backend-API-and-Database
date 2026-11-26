export default function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null
  const pages = Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
    if (totalPages <= 7) return i
    if (page < 4) return i
    if (page > totalPages - 4) return totalPages - 7 + i
    return page - 3 + i
  })

  return (
    <div className="pagination">
      <button
        className="pagination__btn"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
      >‹</button>
      {pages.map((p) => (
        <button
          key={p}
          className={`pagination__btn ${p === page ? 'pagination__btn--active' : ''}`}
          onClick={() => onPageChange(p)}
        >{p + 1}</button>
      ))}
      <button
        className="pagination__btn"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >›</button>
    </div>
  )
}
