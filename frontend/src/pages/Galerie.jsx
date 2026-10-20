import { useEffect, useState } from 'react'
import { getCreators } from '../api/catalog'
import styles from './Galerie.module.css'

const PAGE_SIZE = 12

export default function Galerie() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    getCreators({ page, size: PAGE_SIZE })
      .then(setData)
      .catch(() => setError('Impossible de charger la galerie.'))
      .finally(() => setLoading(false))
  }, [page])

  function goToPrev() {
    setLoading(true)
    setError(null)
    setPage((p) => p - 1)
  }

  function goToNext() {
    setLoading(true)
    setError(null)
    setPage((p) => p + 1)
  }

  const totalPages = data ? Math.ceil(data.totalElements / PAGE_SIZE) : 0

  return (
    <main className={styles.container}>
      <h1>Galerie des créateurs</h1>

      {error && <p role="alert" className={styles.error}>{error}</p>}

      {loading && <p>Chargement…</p>}

      {!loading && !error && data && data.items.length === 0 && (
        <p className={styles.empty}>Aucun créateur pour le moment.</p>
      )}

      {!loading && !error && data && data.items.length > 0 && (
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
            <button onClick={goToPrev} disabled={page === 0} aria-label="Page précédente">
              Précédent
            </button>
            <span>Page {page + 1} sur {totalPages}</span>
            <button onClick={goToNext} disabled={page >= totalPages - 1} aria-label="Page suivante">
              Suivant
            </button>
          </div>
        </>
      )}
    </main>
  )
}
