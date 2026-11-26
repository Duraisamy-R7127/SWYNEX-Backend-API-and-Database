export default function StatCard({ icon: Icon, label, value, color = 'blue', sub }) {
  return (
    <div className={`stat-card stat-card--${color}`}>
      <div className="stat-card__icon"><Icon size={22} /></div>
      <div className="stat-card__body">
        <div className="stat-card__value">{value ?? '—'}</div>
        <div className="stat-card__label">{label}</div>
        {sub && <div className="stat-card__sub">{sub}</div>}
      </div>
    </div>
  )
}
