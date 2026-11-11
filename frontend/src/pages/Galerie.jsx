import { useEffect, useState } from 'react'
import { getCreators } from '../api/catalog'
import styles from './Galerie.module.css'

const PAGE_SIZE = 12

export default function Galerie() {
  // Un nouvel objet à chaque demande : relancer la même page redéclenche le chargement
  const [request, setRequest] = useState({ page: 0 })
  // Dernière page chargée avec succès : reste affichée sous l'erreur
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let ignore = false
    getCreators({ page: request.page, size: PAGE_SIZE })
      .then((result) => {
        if (!ignore) setData(result)
      })
      .catch(() => {
        if (!ignore) setError('Impossible de charger la galerie.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [request])

  function load(page) {
    setLoading(true)
    setError(null)
    setRequest({ page })
  }

  const shownPage = data ? data.page : 0
  const totalPages = data ? Math.ceil(data.totalElements / PAGE_SIZE) : 0

  return (
    <main className={styles.container}>
      <h1>Galerie des créateurs</h1>

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
        <p className={styles.empty}>Aucun créateur pour le moment.</p>
      )}

      {!loading && data && data.items.length > 0 && (
        <>
          <ul className={styles.grid} aria-label="Liste des créateurs">
            {data.items.map((creator) => (
              <li key={creator.userId} className={styles.card}>
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
