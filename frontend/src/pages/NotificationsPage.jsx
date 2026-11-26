import { useEffect, useState } from 'react'
import { notificationApi } from '../api/endpoints'
import { PageLoader } from '../components/ui/Loaders'
import Pagination from '../components/ui/Pagination'
import toast from 'react-hot-toast'
import { Bell, CheckCheck, Circle } from 'lucide-react'

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const fetchNotifications = (p = 0) => {
    setLoading(true)
    notificationApi.getNotifications(p, 20)
      .then(r => {
        setNotifications(r.data.data.content)
        setTotalPages(r.data.data.totalPages)
        setPage(p)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchNotifications() }, [])

  const markRead = async (id) => {
    try {
      await notificationApi.markAsRead(id)
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n))
    } catch {}
  }

  const markAllRead = async () => {
    try {
      await notificationApi.markAllAsRead()
      setNotifications(prev => prev.map(n => ({ ...n, read: true })))
      toast.success('All notifications marked as read')
    } catch {}
  }

  const unreadCount = notifications.filter(n => !n.read).length

  if (loading) return <PageLoader />

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">Notifications</h1>
          <p className="page__sub">{unreadCount > 0 ? `${unreadCount} unread` : 'All caught up!'}</p>
        </div>
        {unreadCount > 0 && (
          <button className="btn btn--outline" onClick={markAllRead}>
            <CheckCheck size={16} /> Mark All Read
          </button>
        )}
      </div>

      {notifications.length === 0
        ? <div className="empty-state empty-state--lg">
            <Bell size={56} />
            <h3>No notifications</h3>
            <p>You're all caught up! Notifications will appear here when your claims are reviewed.</p>
          </div>
        : <div className="notif-page-list">
            {notifications.map(n => (
              <div key={n.id} className={`notif-item ${!n.read ? 'notif-item--unread' : ''}`}>
                <div className="notif-item__icon">
                  {n.read ? <Bell size={18} /> : <Circle size={10} className="notif-item__dot" />}
                </div>
                <div className="notif-item__body">
                  <p className="notif-item__msg">{n.message}</p>
                  <span className="notif-item__time">{new Date(n.createdAt).toLocaleString()}</span>
                </div>
                {!n.read && (
                  <button className="btn btn--ghost btn--sm notif-item__read-btn" onClick={() => markRead(n.id)}>
                    Mark read
                  </button>
                )}
              </div>
            ))}
          </div>
      }

      <Pagination page={page} totalPages={totalPages} onPageChange={fetchNotifications} />
    </div>
  )
}
