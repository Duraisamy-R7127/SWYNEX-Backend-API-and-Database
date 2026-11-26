import { MapPin, Calendar, Tag, User, Eye } from 'lucide-react'
import { Link } from 'react-router-dom'

const STATUS_COLORS = { OPEN: 'green', CLAIMED: 'yellow', RETURNED: 'blue' }
const STATUS_LABELS = { OPEN: 'Available', CLAIMED: 'Claim Approved', RETURNED: 'Returned' }
const TYPE_COLORS = { LOST: 'red', FOUND: 'teal' }

export default function ItemCard({ item }) {
  const imgSrc = item.imageUrl ? `http://localhost:8080${item.imageUrl}` : null

  return (
    <div className="item-card">
      <div className="item-card__img-wrap">
        {imgSrc
          ? <img src={imgSrc} alt={item.itemName} className="item-card__img" />
          : <div className="item-card__img-placeholder"><Tag size={32} /></div>
        }
        <span className={`item-card__badge item-card__badge--${TYPE_COLORS[item.itemType]}`}>
          {item.itemType}
        </span>
        <span className={`item-card__status item-card__status--${STATUS_COLORS[item.status]}`}>
          {STATUS_LABELS[item.status] || item.status}
        </span>
      </div>
      <div className="item-card__body">
        <h3 className="item-card__title">{item.itemName}</h3>
        <p className="item-card__category">{item.category}</p>
        <p className="item-card__desc">{item.description}</p>
        <div className="item-card__meta">
          <span><MapPin size={13} />{item.location}</span>
          <span><Calendar size={13} />{item.eventDate}</span>
          <span><User size={13} />{item.postedByName}</span>
        </div>
        <Link to={`/items/${item.id}`} className="btn btn--primary btn--sm item-card__btn">
          <Eye size={14} /> View Details
        </Link>
      </div>
    </div>
  )
}
