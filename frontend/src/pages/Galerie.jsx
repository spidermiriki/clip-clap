import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getCategories, getCreators } from '../api/catalog'
import styles from './Galerie.module.css'

const PAGE_SIZE = 12

// Les filtres vivent dans l'URL (?ville=&pays=&categorie=&media=) : un rechargement ou un lien partagé les garde
function filtresDepuisUrl(searchParams) {
  return {
    city: searchParams.get('ville') ?? '',
    country: searchParams.get('pays') ?? '',
    categoryId: searchParams.get('categorie') ?? '',
    mediaType: searchParams.get('media') ?? '',
  }
}

export default function Galerie() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { city, country, categoryId, mediaType } = filtresDepuisUrl(searchParams)
  const filtresActifs = Boolean(city || country || categoryId || mediaType)

  const [form, setForm] = useState(() => filtresDepuisUrl(searchParams))
  const [categories, setCategories] = useState([])

  // Un nouvel objet à chaque demande : relancer la même page redéclenche le chargement
  const [request, setRequest] = useState({ page: 0 })
  // Dernière page chargée avec succès : reste affichée sous l'erreur
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // L'URL peut changer sans passer par le formulaire (bouton Précédent ou Suivant du navigateur) :
  // on réaligne alors les champs sur les filtres réellement appliqués et on repart de la page 1,
  // pendant le rendu plutôt que dans un effet, pour éviter un rendu intermédiaire incohérent.
  const urlCourante = searchParams.toString()
  const [urlDuFormulaire, setUrlDuFormulaire] = useState(urlCourante)
  if (urlDuFormulaire !== urlCourante) {
    setUrlDuFormulaire(urlCourante)
    setForm(filtresDepuisUrl(searchParams))
    setLoading(true)
    setError(null)
    setRequest({ page: 0 })
  }

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(() => setCategories([]))
  }, [])

  useEffect(() => {
    let ignore = false
    getCreators({ page: request.page, size: PAGE_SIZE, city, country, categoryId, mediaType })
      .then((result) => {
        if (!ignore) setData(result)
      })
      .catch((err) => {
        // Erreur HTTP : message du backend ; panne réseau (TypeError) : message générique
        if (!ignore) setError(err?.status ? err.message : 'Impossible de charger la galerie.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [request, city, country, categoryId, mediaType])

  function load(page) {
    setLoading(true)
    setError(null)
    setRequest({ page })
  }

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function handleFiltrer(e) {
    e.preventDefault()
    const next = new URLSearchParams()
    if (form.city.trim()) next.set('ville', form.city.trim())
    if (form.country.trim()) next.set('pays', form.country.trim())
    if (form.categoryId) next.set('categorie', form.categoryId)
    if (form.mediaType) next.set('media', form.mediaType)
    setSearchParams(next)
    // Un nouveau filtre repart de la première page
    load(0)
  }

  const shownPage = data ? data.page : 0
  const totalPages = data ? Math.ceil(data.totalElements / PAGE_SIZE) : 0

  return (
    <main className={styles.container}>
      <h1>Galerie des créateurs</h1>
      <p className={styles.accroche}>Trouvez un photographe ou un vidéaste pour votre projet.</p>

      <form className={styles.filtres} onSubmit={handleFiltrer} role="search">
        <label>
          Type de prestation
          <select name="categoryId" value={form.categoryId} onChange={handleChange}>
            <option value="">Tous les types</option>
            {categories.map((categorie) => (
              <option key={categorie.id} value={String(categorie.id)}>{categorie.name}</option>
            ))}
          </select>
        </label>
        <label>
          Ville
          <input name="city" type="text" value={form.city} onChange={handleChange} />
        </label>
        <label>
          Pays
          <input name="country" type="text" value={form.country} onChange={handleChange} />
        </label>
        <label>
          Type de média
          <select name="mediaType" value={form.mediaType} onChange={handleChange}>
            <option value="">Photo ou vidéo</option>
            <option value="PHOTO">Photo</option>
            <option value="VIDEO">Vidéo</option>
          </select>
        </label>
        <button type="submit">Filtrer</button>
      </form>

      {error && (
        <div className={styles.errorBox}>
          <p role="alert" className={styles.error}>{error}</p>
          <button type="button" onClick={() => load(request.page)}>
            Réessayer
          </button>
        </div>
      )}

      {loading && <p>Chargement…</p>}

      {!loading && data && data.items.length === 0 && (
        <p className={styles.empty}>
          {filtresActifs ? 'Aucun créateur ne correspond à ces filtres.' : 'Aucun créateur pour le moment.'}
        </p>
      )}

      {!loading && data && data.items.length > 0 && (
        <>
          <ul className={styles.grid} aria-label="Liste des créateurs">
            {data.items.map((creator) => (
              <li key={creator.userId}>
                <Link to={`/createurs/${creator.userId}`} className={styles.card}>
                  <div className={styles.avatar}>
                    {creator.photoUrl ? (
                      <img src={creator.photoUrl} alt={`Photo de ${creator.displayName}`} />
                    ) : (
                      <span aria-hidden="true">{creator.displayName.charAt(0).toUpperCase()}</span>
                    )}
                  </div>
                  <p className={styles.name}>{creator.displayName}</p>
                  {(creator.city || creator.country) && (
                    <p className={styles.location}>
                      {[creator.city, creator.country].filter(Boolean).join(', ')}
                    </p>
                  )}
                </Link>
              </li>
            ))}
          </ul>

          <div className={styles.pagination}>
            <button onClick={() => load(shownPage - 1)} disabled={shownPage === 0} aria-label="Page précédente">
              Précédent
            </button>
            <span>Page {shownPage + 1} sur {totalPages}</span>
            <button onClick={() => load(shownPage + 1)} disabled={shownPage >= totalPages - 1} aria-label="Page suivante">
              Suivant
            </button>
          </div>
        </>
      )}
    </main>
  )
}
