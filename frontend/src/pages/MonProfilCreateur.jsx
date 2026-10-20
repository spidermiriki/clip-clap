import { useEffect, useState } from 'react'
import { createMyProfile, getMyProfile, updateMyProfile } from '../api/creators'

const EMPTY_FORM = { displayName: '', bio: '', city: '', country: '', serviceAreas: '' }

export default function MonProfilCreateur() {
  const [form, setForm] = useState(EMPTY_FORM)
  const [isCreation, setIsCreation] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)

  useEffect(() => {
    getMyProfile()
      .then((profile) => {
        if (profile) {
          setForm({
            displayName: profile.displayName ?? '',
            bio: profile.bio ?? '',
            city: profile.city ?? '',
            country: profile.country ?? '',
            serviceAreas: profile.serviceAreas ?? '',
          })
          setIsCreation(false)
        } else {
          setIsCreation(true)
        }
      })
      .catch(() => setError('Impossible de charger le profil.'))
      .finally(() => setLoading(false))
  }, [])

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
    setError(null)
    setSuccess(false)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.displayName.trim()) {
      setError('Le nom affiché est obligatoire.')
      return
    }
    setSaving(true)
    setError(null)
    setSuccess(false)
    try {
      if (isCreation) {
        await createMyProfile(form)
        setIsCreation(false)
      } else {
        await updateMyProfile(form)
      }
      setSuccess(true)
    } catch (err) {
      setError(err.message ?? 'Une erreur est survenue.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <p>Chargement…</p>

  return (
    <main>
      <h1>{isCreation ? 'Créer mon profil créateur' : 'Mon profil créateur'}</h1>

      <form onSubmit={handleSubmit} noValidate>
        <div>
          <label htmlFor="displayName">Nom affiché *</label>
          <input
            id="displayName"
            name="displayName"
            type="text"
            value={form.displayName}
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label htmlFor="bio">Bio</label>
          <textarea id="bio" name="bio" value={form.bio} onChange={handleChange} rows={4} />
        </div>

        <div>
          <label htmlFor="city">Ville</label>
          <input id="city" name="city" type="text" value={form.city} onChange={handleChange} />
        </div>

        <div>
          <label htmlFor="country">Pays</label>
          <input id="country" name="country" type="text" value={form.country} onChange={handleChange} />
        </div>

        <div>
          <label htmlFor="serviceAreas">Zones d&apos;intervention</label>
          <input
            id="serviceAreas"
            name="serviceAreas"
            type="text"
            value={form.serviceAreas}
            onChange={handleChange}
          />
        </div>

        {error && <p role="alert" style={{ color: 'red' }}>{error}</p>}
        {success && <p role="status" style={{ color: 'green' }}>Profil enregistré.</p>}

        <button type="submit" disabled={saving}>
          {saving ? 'Enregistrement…' : isCreation ? 'Créer le profil' : 'Enregistrer'}
        </button>
      </form>
    </main>
  )
}
