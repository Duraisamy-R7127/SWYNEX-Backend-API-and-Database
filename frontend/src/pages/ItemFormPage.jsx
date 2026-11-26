import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { itemApi } from '../api/endpoints'
import { useAuth } from '../context/AuthContext'
import toast from 'react-hot-toast'
import { Upload, X, ArrowLeft, AlertCircle } from 'lucide-react'

const CATEGORIES = [
  'Mobile Phones', 'Wallets', 'ID Cards', 'Bags', 'Books',
  'Watches', 'Keys', 'Earphones', 'Electronic Devices', 'Clothing', 'Other Items'
]

const empty = { itemName: '', category: '', description: '', location: '', eventDate: '', additionalDetails: '' }

export default function ItemFormPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const isEdit = Boolean(id)
  const isAdmin = user?.role === 'ADMIN'
  const [form, setForm] = useState(empty)
  const [image, setImage] = useState(null)
  const [preview, setPreview] = useState(null)
  const [removeImage, setRemoveImage] = useState(false)
  const [existingImg, setExistingImg] = useState(null)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  // Redirect admin away - they use the Admin panel to manage found items
  useEffect(() => {
    if (isAdmin && !isEdit) {
      toast.error('Administrators add Found items from the Admin panel.')
      navigate('/admin/found-items')
    }
  }, [isAdmin, isEdit, navigate])

  useEffect(() => {
    if (isEdit) {
      itemApi.getItem(id).then(r => {
        const item = r.data.data
        setForm({
          itemName: item.itemName,
          category: item.category,
          description: item.description,
          location: item.location,
          eventDate: item.eventDate,
          additionalDetails: item.additionalDetails || '',
        })
        if (item.imageUrl) setExistingImg(`http://localhost:8080${item.imageUrl}`)
      }).catch(() => navigate('/my-items'))
    }
  }, [id, isEdit, navigate])

  const validate = () => {
    const e = {}
    if (!form.itemName) e.itemName = 'Item name is required'
    if (!form.category) e.category = 'Category is required'
    if (!form.description || form.description.length < 5) e.description = 'Description is required (min 5 characters)'
    if (!form.location) e.location = 'Location is required'
    if (!form.eventDate) e.eventDate = 'Date is required'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleImageChange = (e) => {
    const file = e.target.files[0]
    if (!file) return
    if (file.size > 5 * 1024 * 1024) { toast.error('Image must be under 5MB'); return }
    setImage(file)
    setPreview(URL.createObjectURL(file))
    setRemoveImage(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return
    setLoading(true)
    try {
      const fd = new FormData()
      fd.append('itemName', form.itemName)
      fd.append('category', form.category)
      fd.append('description', form.description)
      fd.append('location', form.location)
      fd.append('eventDate', form.eventDate)
      fd.append('itemType', 'LOST')
      if (form.additionalDetails) fd.append('additionalDetails', form.additionalDetails)
      if (image) fd.append('image', image)
      if (isEdit) fd.append('removeImage', removeImage)

      if (isEdit) {
        await itemApi.updateItem(id, fd)
        toast.success('Lost report updated successfully!')
      } else {
        await itemApi.createItem(fd)
        toast.success('Lost item reported successfully!')
      }
      navigate('/my-items')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save item')
    } finally {
      setLoading(false)
    }
  }

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <div className="page">
      <div className="page__back">
        <button className="btn btn--ghost btn--sm" onClick={() => navigate(-1)}>
          <ArrowLeft size={16} /> Back
        </button>
      </div>

      <div className="form-page">
        <div className="form-page__header">
          <h1 className="page__title">{isEdit ? 'Edit Lost Report' : 'Report Lost Item'}</h1>
          <p className="page__sub">
            {isEdit ? 'Update lost item details.' : 'Submit details of your lost belonging.'}
          </p>
        </div>

        <form className="item-form" onSubmit={handleSubmit} noValidate>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Item Name *</label>
              <input className={`form-input ${errors.itemName ? 'form-input--error' : ''}`}
                placeholder="e.g. Black Samsung Galaxy S23" value={form.itemName} onChange={set('itemName')} />
              {errors.itemName && <span className="form-error">{errors.itemName}</span>}
            </div>
            <div className="form-group">
              <label className="form-label">Category *</label>
              <select className={`form-select ${errors.category ? 'form-input--error' : ''}`}
                value={form.category} onChange={set('category')}>
                <option value="">Select category</option>
                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
              {errors.category && <span className="form-error">{errors.category}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Description *</label>
            <textarea className={`form-textarea ${errors.description ? 'form-input--error' : ''}`}
              rows={4} placeholder="Describe the item in detail – color, brand, model, any stickers or marks…"
              value={form.description} onChange={set('description')} />
            {errors.description && <span className="form-error">{errors.description}</span>}
            <span className="form-hint">{form.description.length}/2000</span>
          </div>

          <div className="form-group">
            <label className="form-label">Additional Identifying Details <span className="form-optional">(optional)</span></label>
            <textarea className="form-textarea" rows={2}
              placeholder="Any unique marks, serial number, contents, or other details that could prove ownership…"
              value={form.additionalDetails} onChange={set('additionalDetails')} />
            <span className="form-hint">{form.additionalDetails.length}/1000</span>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Where did you lose it? *</label>
              <input className={`form-input ${errors.location ? 'form-input--error' : ''}`}
                placeholder="e.g. Main Library, 2nd Floor" value={form.location} onChange={set('location')} />
              {errors.location && <span className="form-error">{errors.location}</span>}
            </div>
            <div className="form-group">
              <label className="form-label">Date Lost *</label>
              <input type="date" className={`form-input ${errors.eventDate ? 'form-input--error' : ''}`}
                max={new Date().toISOString().split('T')[0]}
                value={form.eventDate} onChange={set('eventDate')} />
              {errors.eventDate && <span className="form-error">{errors.eventDate}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Photo <span className="form-optional">(optional, max 5MB)</span></label>
            {(preview || (existingImg && !removeImage)) ? (
              <div className="img-preview">
                <img src={preview || existingImg} alt="preview" />
                <button type="button" className="img-preview__remove"
                  onClick={() => { setImage(null); setPreview(null); if (isEdit) setRemoveImage(true) }}>
                  <X size={16} />
                </button>
              </div>
            ) : (
              <label className="upload-zone">
                <Upload size={28} />
                <span>Click to upload or drag &amp; drop</span>
                <span className="upload-zone__hint">PNG, JPG, WEBP up to 5MB</span>
                <input type="file" accept="image/*" className="upload-zone__input" onChange={handleImageChange} />
              </label>
            )}
          </div>

          <div className="form-actions">
            <button type="button" className="btn btn--ghost" onClick={() => navigate(-1)}>Cancel</button>
            <button type="submit" className="btn btn--primary" disabled={loading}>
              {loading ? 'Saving…' : isEdit ? 'Update Report' : 'Submit Lost Report'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
