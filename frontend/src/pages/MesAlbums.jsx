import { useEffect, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { Link } from 'react-router-dom'
import { createAlbum, deleteAlbum, deletePhoto, FORMATS_ACCEPTES, listMyAlbums, renameAlbum, uploadPhoto } from '../api/albums'
import styles from './MesAlbums.module.css'

// « photo 2 » ou « vidéo 2 » : libellé d'un média de l'album (#94)
const nature = (media) => (media.mediaType === 'VIDEO' ? 'vidéo' : 'photo')

// Gestion des albums du créateur connecté (wireframe 10, #15), photos et vidéos (#94)
export default function MesAlbums() {
  const { user } = useAuth()
  const token = user?.access_token ?? null

  const [albums, setAlbums] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [sansProfil, setSansProfil] = useState(false)
  const [error, setError] = useState(null)
  const [nouveauNom, setNouveauNom] = useState('')
  const [renommage, setRenommage] = useState(null)
  const [envoi, setEnvoi] = useState(null)

  // Relancé à chaque nouveau token (renouvellement silencieux de la session) : on repart d'un état
  // propre, et une réponse arrivée après une plus récente est ignorée (#129, comme #119)
  useEffect(() => {
    let ignore = false
    setError(null)
    setSansProfil(false)
    listMyAlbums(token)
      .then((result) => {
        if (ignore) return
        setAlbums(result)
        setSelectedId(result[0]?.id ?? null)
      })
      .catch((err) => {
        if (ignore) return
        if (err?.status === 404) setSansProfil(true)
        else setError(err?.message ?? 'Impossible de charger vos albums.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [token])

  // Sans sélection valide (ex. après la suppression de l'album affiché), le premier album est montré
  const album = albums.find((a) => a.id === selectedId) ?? albums[0] ?? null

  function majAlbum(albumId, changer) {
    setAlbums((prev) => prev.map((a) => (a.id === albumId ? changer(a) : a)))
  }

  async function handleCreer(e) {
    e.preventDefault()
    setError(null)
    try {
      const cree = await createAlbum(nouveauNom, token)
      setAlbums((prev) => [...prev, cree])
      setSelectedId(cree.id)
      setNouveauNom('')
    } catch (err) {
      setError(err?.message ?? "Impossible de créer l'album.")
    }
  }

  async function handleRenommer(e) {
    e.preventDefault()
    setError(null)
    try {
      await renameAlbum(album.id, renommage, token)
      majAlbum(album.id, (a) => ({ ...a, name: renommage.trim() }))
      setRenommage(null)
    } catch (err) {
      setError(err?.message ?? "Impossible de renommer l'album.")
    }
  }

  async function envoyer(fichiers) {
    const liste = Array.from(fichiers)
    if (!album || liste.length === 0) return
    setError(null)
    const albumId = album.id
    try {
      for (const [index, fichier] of liste.entries()) {
        setEnvoi(`Envoi du fichier ${index + 1} sur ${liste.length}…`)
        const photo = await uploadPhoto(albumId, fichier, token)
        majAlbum(albumId, (a) => ({ ...a, photos: [...a.photos, photo] }))
      }
    } catch (err) {
      setError(err?.message ?? "L'envoi du fichier a échoué.")
    } finally {
      setEnvoi(null)
    }
  }

  async function supprimerPhoto(photo, index) {
    if (!window.confirm(`Supprimer la ${nature(photo)} ${index + 1} de « ${album.name} » ?`)) return
    setError(null)
    const albumId = album.id
    try {
      await deletePhoto(albumId, photo.id, token)
      majAlbum(albumId, (a) => ({ ...a, photos: a.photos.filter((p) => p.id !== photo.id) }))
    } catch (err) {
      setError(err?.message ?? `Impossible de supprimer la ${nature(photo)}.`)
    }
  }

  async function supprimerAlbum() {
    if (!window.confirm(`Supprimer l'album « ${album.name} » et toutes ses photos ?`)) return
    setError(null)
    const albumId = album.id
    try {
      await deleteAlbum(albumId, token)
      // Forme fonctionnelle : l'état a pu changer pendant la requête (revue de #125)
      setAlbums((prev) => prev.filter((a) => a.id !== albumId))
      setSelectedId((id) => (id === albumId ? null : id))
      setRenommage(null)
    } catch (err) {
      setError(err?.message ?? "Impossible de supprimer l'album.")
    }
  }

  function handleDrop(e) {
    e.preventDefault()
    if (!envoi) envoyer(e.dataTransfer.files)
  }

  if (loading) return <main className={styles.container}><p>Chargement…</p></main>

  if (sansProfil) {
    return (
      <main className={styles.container}>
        <h1>Mes albums</h1>
        <p>Créez d&apos;abord votre profil créateur pour pouvoir ajouter des albums.</p>
        <Link to="/mon-profil">Créer mon profil</Link>
      </main>
    )
  }

  return (
    <main className={styles.container}>
      <h1>Mes albums</h1>
      {error && <p role="alert" className={styles.error}>{error}</p>}

      <div className={styles.layout}>
        <aside className={styles.liste}>
          {albums.length === 0 ? (
            <p className={styles.vide}>Aucun album pour le moment.</p>
          ) : (
            <ul aria-label="Mes albums">
              {albums.map((a) => (
                <li key={a.id}>
                  <button
                    type="button"
                    aria-current={a.id === album?.id ? 'true' : undefined}
                    onClick={() => { setSelectedId(a.id); setRenommage(null) }}
                  >
                    {a.name} <span className={styles.compte}>({a.photos.length})</span>
                  </button>
                </li>
              ))}
            </ul>
          )}

          <form onSubmit={handleCreer} className={styles.nouvel}>
            <label>
              Nom du nouvel album
              <input value={nouveauNom} onChange={(e) => setNouveauNom(e.target.value)} maxLength={100} />
            </label>
            <button type="submit">+ Nouvel album</button>
          </form>
        </aside>

        {album && (
          <section className={styles.detail} aria-label={`Album ${album.name}`}>
            <div className={styles.entete}>
              {renommage === null ? (
                <>
                  <h2>{album.name}</h2>
                  <button type="button" onClick={() => setRenommage(album.name)}>Renommer</button>
                  <button type="button" onClick={supprimerAlbum}>Supprimer l&apos;album</button>
                </>
              ) : (
                <form onSubmit={handleRenommer} className={styles.renommer}>
                  <label>
                    Nouveau nom
                    <input value={renommage} onChange={(e) => setRenommage(e.target.value)} maxLength={100} autoFocus />
                  </label>
                  <button type="submit">Enregistrer</button>
                  <button type="button" onClick={() => setRenommage(null)}>Annuler</button>
                </form>
              )}
            </div>

            <label className={styles.depot} onDragOver={(e) => e.preventDefault()} onDrop={handleDrop}>
              <span>{envoi ?? "Déposez vos photos et vidéos ici ou cliquez pour les choisir (photos JPEG, PNG ou WebP jusqu'à 10 Mo ; vidéos MP4, WebM ou MOV jusqu'à 100 Mo)"}</span>
              <input
                type="file"
                accept={FORMATS_ACCEPTES.join(',')}
                multiple
                disabled={Boolean(envoi)}
                onChange={(e) => { envoyer(e.target.files); e.target.value = '' }}
              />
            </label>

            {album.photos.length === 0 ? (
              <p className={styles.vide}>Cet album ne contient pas encore de photo.</p>
            ) : (
              <ul className={styles.photos} aria-label={`Photos de ${album.name}`}>
                {album.photos.map((photo, index) => (
                  <li key={photo.id} className={styles.photo}>
                    {photo.mediaType === 'VIDEO' ? (
                      <video src={photo.url} controls preload="metadata" aria-label={`${album.name}, vidéo ${index + 1}`} />
                    ) : (
                      <img src={photo.url} alt={`${album.name}, photo ${index + 1}`} loading="lazy" />
                    )}
                    <button
                      type="button"
                      className={styles.supprimer}
                      onClick={() => supprimerPhoto(photo, index)}
                      aria-label={`Supprimer la ${nature(photo)} ${index + 1}`}
                    >
                      Supprimer
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}
      </div>
    </main>
  )
}
