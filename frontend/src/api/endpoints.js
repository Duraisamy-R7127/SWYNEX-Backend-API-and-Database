import api from './axios'

export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
}

export const userApi = {
  getMe: () => api.get('/users/me'),
  updateMe: (data) => api.put('/users/me', data),
  getMyItems: (page = 0, size = 10) => api.get(`/users/me/items?page=${page}&size=${size}`),
  getMyClaims: () => api.get('/users/me/claims'),
  getUser: (id) => api.get(`/users/${id}`),
}

export const itemApi = {
  getItems: (params) => api.get('/items', { params }),
  getItem: (id) => api.get(`/items/${id}`),
  getMatches: () => api.get('/items/matches'),
  getItemMatches: (id) => api.get(`/items/${id}/matches`),
  // For users: report a LOST item
  createItem: (formData) => api.post('/items', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  updateItem: (id, formData) => api.put(`/items/${id}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  deleteItem: (id) => api.delete(`/items/${id}`),
}

export const claimApi = {
  submitClaim: (data) => api.post('/claims', data),
  getMyClaims: () => api.get('/claims/my'),
  getClaim: (id) => api.get(`/claims/${id}`),
  getClaimStatus: (id) => api.get(`/claims/${id}/status`),
}

export const notificationApi = {
  getNotifications: (page = 0, size = 20) => api.get(`/notifications?page=${page}&size=${size}`),
  getUnreadCount: () => api.get('/notifications/unread-count'),
  markAsRead: (id) => api.patch(`/notifications/${id}/read`),
  markAllAsRead: () => api.patch('/notifications/read-all'),
}

export const adminApi = {
  getDashboard: () => api.get('/admin/dashboard'),
  getUsers: (page = 0, size = 10) => api.get(`/admin/users?page=${page}&size=${size}`),
  // All items (admin overview)
  getItems: (params) => api.get('/admin/items', { params }),
  // Found items management
  getFoundItems: (params) => api.get('/admin/found-items', { params }),
  addFoundItem: (formData) => api.post('/admin/found-items', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  updateFoundItem: (id, formData) => api.put(`/admin/found-items/${id}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  deleteFoundItem: (id) => api.delete(`/admin/found-items/${id}`),
  // Lost reports from students
  getLostReports: (params) => api.get('/admin/lost-reports', { params }),
  // Claims management
  getClaims: (params) => api.get('/admin/claims', { params }),
  getPendingClaims: (page = 0, size = 10) => api.get(`/admin/claims/pending?page=${page}&size=${size}`),
  verifyClaim: (id, data) => api.patch(`/admin/claims/${id}/verify`, data),
  markItemReturned: (id) => api.patch(`/admin/items/${id}/returned`),
  updateItemStatus: (id, status) => api.patch(`/admin/items/${id}/status`, { status }),
}
