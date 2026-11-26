import { Loader2 } from 'lucide-react'

export function Spinner({ size = 20, className = '' }) {
  return <Loader2 size={size} className={`spinner ${className}`} />
}

export function PageLoader() {
  return (
    <div className="page-loader">
      <Spinner size={36} />
    </div>
  )
}

export function SkeletonCard() {
  return (
    <div className="skeleton-card">
      <div className="skeleton skeleton--img" />
      <div className="skeleton-card__body">
        <div className="skeleton skeleton--title" />
        <div className="skeleton skeleton--text" />
        <div className="skeleton skeleton--text skeleton--short" />
      </div>
    </div>
  )
}

export function SkeletonRow() {
  return (
    <div className="skeleton-row">
      <div className="skeleton skeleton--circle" />
      <div className="skeleton-row__lines">
        <div className="skeleton skeleton--text" />
        <div className="skeleton skeleton--text skeleton--short" />
      </div>
    </div>
  )
}
