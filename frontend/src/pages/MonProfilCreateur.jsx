import { useEffect, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { getCategories } from '../api/catalog'
import { createMyProfile, getMyCategories, getMyProfile, updateMyCategories, updateMyProfile } from '../api/creators'
import styles from './MonProfilCreateur.module.css'

const EMPTY_FORM = { displayName: '', bio: '', city: '', country: '', serviceAreas: '' }

// Statut de validation du profil par l'administrateur (#33) : rien à signaler une fois validé
function BandeauValidation({ statut }) {
  if (statut?.validationStatus === 'PENDING') {
    return (
      <p role="status" className={styles.bandeau}>
        Votre profil est en attente de validation par un administrateur : il n&apos;apparaîtra dans la
        galerie qu&apos;une fois validé. Vous pouvez déjà préparer vos albums et vos prestations.
      </p>
    )
  }
  if (statut?.validationStatus === 'REJECTED') {
    return (
      <p role="status" className={`${styles.bandeau} ${styles.refuse}`}>
        Votre profil a été refusé : {statut.rejectionReason}. Corrigez-le puis enregistrez : il repassera en
        attente de validation.
      </p>
    )
  }
  return null
}

export default function MonProfilCreateur() {
  const { user } = useAuth()
  const accessToken = user?.access_token ?? null

  const [form, setForm] = useState(EMPTY_FORM)
  const [isCreation, setIsCreation] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)
  const [statut, setStatut] = useState(null)
  // Types de prestation (#87) : la liste vient de catalog, le choix de creators
  const [categories, setCategories] = useState([])
  const [categoriesIndisponibles, setCategoriesIndisponibles] = useState(false)
  const [choix, setChoix] = useState([])
  // Vrai quand le choix affiché est le vrai (lu, ou profil nouveau) : sinon on ne l'enregistre pas
  const [choixPret, setChoixPret] = useState(false)
  // Profil existant illisible : pas de formulaire, pour ne jamais l'écraser avec des champs vides
  const [profilIndisponible, setProfilIndisponible] = useState(false)

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(() => setCategoriesIndisponibles(true))
  }, [])

  useEffect(() => {
    getMyProfile(accessToken)
      .then((profile) => {
        if (profile) {
          setStatut(profile)
          setForm({
            displayName: profile.displayName ?? '',
            bio: profile.bio ?? '',
            city: profile.city ?? '',
            country: profile.country ?? '',
            serviceAreas: profile.serviceAreas ?? '',
          })
          setIsCreation(false)
          // À part, après le formulaire : leur échec ne vide jamais le profil (revue de #122).
          // Sans eux, l'enregistrement ne touche pas aux catégories (voir handleSubmit).
          getMyCategories(accessToken)
            .then((ids) => {
              setChoix(ids)
              setChoixPret(true)
            })
            .catch(() => setCategoriesIndisponibles(true))
        } else {
          setIsCreation(true)
          setChoixPret(true)
        }
      })
      .catch(() => setProfilIndisponible(true))
      .finally(() => setLoading(false))
  }, [accessToken])

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
    setError(null)
    setSuccess(false)
  }

  function basculer(categoryId) {
    setChoix((prev) => (prev.includes(categoryId) ? prev.filter((id) => id !== categoryId) : [...prev, categoryId]))
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
        setStatut(await createMyProfile(form, accessToken))
        setIsCreation(false)
      } else {
        setStatut(await updateMyProfile(form, accessToken))
      }
      // Après le profil : à la création, il doit exister avant ses catégories
      if (choixPret && !categoriesIndisponibles) setChoix(await updateMyCategories(choix, accessToken))
      setSuccess(true)
    } catch (err) {
      setError(err.message ?? 'Une erreur est survenue.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <p>Chargement…</p>

  if (profilIndisponible) {
    return (
      <main className={styles.container}>
        <h1>Mon profil créateur</h1>
        <p role="alert" className={styles.error}>Impossible de charger le profil. Rechargez la page pour réessayer.</p>
      </main>
    )
  }

  return (
    <main className={styles.container}>
      <h1>{isCreation ? 'Créer mon profil créateur' : 'Mon profil créateur'}</h1>
      <BandeauValidation statut={statut} />

      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <div className={styles.field}>
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

        <div className={styles.field}>
          <label htmlFor="bio">Bio</label>
          <textarea id="bio" name="bio" value={form.bio} onChange={handleChange} rows={4} />
        </div>

        <div className={styles.field}>
          <label htmlFor="city">Ville</label>
          <input id="city" name="city" type="text" value={form.city} onChange={handleChange} />
        </div>

        <div className={styles.field}>
          <label htmlFor="country">Pays</label>
          <input id="country" name="country" type="text" value={form.country} onChange={handleChange} />
        </div>

        <div className={styles.field}>
          <label htmlFor="serviceAreas">Zones d&apos;intervention</label>
          <input
            id="serviceAreas"
            name="serviceAreas"
            type="text"
            placeholder="ex. Mons, Charleroi, Bruxelles"
            value={form.serviceAreas}
            onChange={handleChange}
          />
        </div>

        <fieldset className={styles.field}>
          <legend>Types de prestation</legend>
          {categoriesIndisponibles ? (
            <p>Les types de prestation ne peuvent pas être chargés pour le moment.</p>
          ) : (
            <div className={styles.categories}>
              {categories.map((category) => (
                <label key={category.id}>
                  <input
                    type="checkbox"
                    checked={choix.includes(category.id)}
                    onChange={() => basculer(category.id)}
                  />
                  {category.name}
                </label>
              ))}
            </div>
          )}
        </fieldset>

        {error && <p role="alert" className={styles.error}>{error}</p>}
        {success && <p role="status" className={styles.success}>Profil enregistré.</p>}

        <button type="submit" className={styles.submitButton} disabled={saving}>
          {saving ? 'Enregistrement…' : isCreation ? 'Créer le profil' : 'Enregistrer'}
        </button>
      </form>
    </main>
  )
}
