import { useEffect, useState } from 'react'
import { adminApi } from '../../api/endpoints'
import Pagination from '../../components/ui/Pagination'
import { PageLoader } from '../../components/ui/Loaders'
import { Users, Shield, User, Calendar, Mail, Phone } from 'lucide-react'

export default function AdminUsersPage() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const fetchUsers = (p = 0) => {
    setLoading(true)
    adminApi.getUsers(p, 15)
      .then(r => {
        setUsers(r.data.data.content)
        setTotalPages(r.data.data.totalPages)
        setTotalElements(r.data.data.totalElements)
        setPage(p)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchUsers() }, [])

  if (loading) return <PageLoader />

  return (
    <div className="page">
      <div className="page__header">
        <div>
          <h1 className="page__title">Users</h1>
          <p className="page__sub">{totalElements} registered users</p>
        </div>
      </div>

      <div className="table-card">
        <table className="data-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Role</th>
              <th>Joined</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id}>
                <td>
                  <div className="table-user">
                    <div className="avatar avatar--sm">{u.name[0].toUpperCase()}</div>
                    <span>{u.name}</span>
                  </div>
                </td>
                <td><span className="table-email"><Mail size={13} />{u.email}</span></td>
                <td>{u.phone || <span className="text-muted">—</span>}</td>
                <td>
                  <span className={`role-badge role-badge--${u.role.toLowerCase()}`}>
                    {u.role === 'ADMIN' ? <Shield size={12} /> : <User size={12} />} {u.role}
                  </span>
                </td>
                <td className="table-date">{new Date(u.createdAt).toLocaleDateString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Pagination page={page} totalPages={totalPages} onPageChange={fetchUsers} />
    </div>
  )
}
