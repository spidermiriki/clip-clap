import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getCreatorProfile } from '../api/catalog'
import { formatDuree, formatPrix } from '../format'
import styles from './ProfilPublic.module.css'

// Profil public d'un créateur (wireframe 04, #17), ses prestations (#27) et ses albums (#15)
export default function ProfilPublic() {
  const { userId } = useParams()
  // Un nouvel objet à chaque demande : « Réessayer » relance le chargement
  const [request, setRequest] = useState({ userId })
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  if (request.userId !== userId) {
    // Navigation d'un profil à un autre : on repart de zéro pendant le rendu, sans effet en cascade
    setRequest({ userId })
    setProfile(null)
    setLoading(true)
    setError(null)
  }

  useEffect(() => {
    let ignore = false
    getCreatorProfile(request.userId)
      .then((result) => {
        if (!ignore) setProfile(result)
      })
      .catch((err) => {
        if (ignore) return
        setError(err?.status === 404
          ? { introuvable: true, message: "Ce créateur n'existe pas ou n'est plus visible." }
          : { introuvable: false, message: err?.status ? err.message : 'Impossible de charger ce profil.' })
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [request])

  function retry() {
    setLoading(true)
    setError(null)
    setRequest({ userId })
  }

  if (loading) return <main className={styles.container}><p>Chargement…</p></main>

  if (error) {
    return (
      <main className={styles.container}>
        <p role="alert" className={styles.error}>{error.message}</p>
        <div className={styles.actions}>
          {!error.introuvable && <button type="button" onClick={retry}>Réessayer</button>}
          <Link to="/">Retour à la galerie</Link>
        </div>
      </main>
    )
  }

  const lieu = [profile.city, profile.country].filter(Boolean).join(', ')

  return (
    <main className={styles.container}>
      <Link to="/" className={styles.retour}>← Galerie</Link>

      <header className={styles.entete}>
        <div className={styles.avatar}>
          {profile.photoUrl ? (
            <img src={profile.photoUrl} alt={`Photo de ${profile.displayName}`} />
          ) : (
            <span aria-hidden="true">{profile.displayName.charAt(0).toUpperCase()}</span>
          )}
        </div>
        <div>
          <h1>{profile.displayName}</h1>
          {lieu && <p className={styles.lieu}>{lieu}</p>}
        </div>
      </header>

      <section aria-labelledby="titre-bio">
        <h2 id="titre-bio">À propos</h2>
        <p>{profile.bio || "Ce créateur n'a pas encore rédigé sa présentation."}</p>
      </section>

      <section aria-labelledby="titre-zones">
        <h2 id="titre-zones">Zones d&apos;intervention</h2>
        <p>{profile.serviceAreas || 'Non précisées.'}</p>
      </section>

      <section aria-labelledby="titre-prestations">
        <h2 id="titre-prestations">Prestations</h2>
        {profile.offerings?.length ? (
          <ul className={styles.prestations} aria-label="Prestations proposées">
            {profile.offerings.map((offering) => (
              <li key={offering.id}>
                <h3>{offering.name}</h3>
                <p>{formatDuree(offering.durationMinutes)} · {formatPrix(offering.basePrice)}</p>
                {offering.options.length > 0 && (
                  <ul aria-label={`Options de ${offering.name}`}>
                    {offering.options.map((option) => (
                      <li key={option.id}>{option.name} : + {formatPrix(option.extraPrice)}</li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        ) : (
          <p>Aucune prestation pour le moment.</p>
        )}
      </section>

      <section aria-labelledby="titre-albums">
        <h2 id="titre-albums">Albums</h2>
        {profile.albums?.length ? (
          profile.albums.map((album) => (
            <div key={album.id} className={styles.album}>
              <h3>{album.name}</h3>
              {album.photos.length ? (
                <ul className={styles.photos} aria-label={`Photos de l'album ${album.name}`}>
                  {album.photos.map((photo, index) => (
                    <li key={photo.id}>
                      <img src={photo.url} alt={`${album.name}, photo ${index + 1}`} loading="lazy" />
                    </li>
                  ))}
                </ul>
              ) : (
                <p>Cet album ne contient pas encore de photo.</p>
              )}
            </div>
          ))
        ) : (
          <p>Aucun album pour le moment.</p>
        )}
      </section>
    </main>
  )
}
