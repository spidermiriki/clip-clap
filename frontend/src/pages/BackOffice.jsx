import { useEffect, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { Link } from 'react-router-dom'
import { listPendingCreators, rejectCreator, validateCreator } from '../api/admin'
import styles from './BackOffice.module.css'

// Back-office administrateur (wireframe 11) : validation des comptes créateurs (#33)
export default function BackOffice() {
  const { user } = useAuth()
  const token = user?.access_token ?? null

  const [enAttente, setEnAttente] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [message, setMessage] = useState(null)
  // Refus en cours de saisie : { userId, motif }
  const [refus, setRefus] = useState(null)
  // Compte dont la décision est en cours d'envoi : ses boutons sont désactivés (pas de double envoi)
  const [enCours, setEnCours] = useState(null)

  useEffect(() => {
    listPendingCreators(token)
      .then(setEnAttente)
      .catch((err) => setError(err?.message ?? 'Impossible de charger les comptes en attente.'))
      .finally(() => setLoading(false))
  }, [token])

  function retirer(userId, texte) {
    setEnAttente((prev) => prev.filter((c) => c.userId !== userId))
    setMessage(texte)
    setRefus(null)
  }

  async function valider(creator) {
    setError(null)
    setEnCours(creator.userId)
    try {
      await validateCreator(creator.userId, token)
      retirer(creator.userId, `${creator.displayName} est validé : son profil apparaît dans la galerie.`)
    } catch (err) {
      setError(err?.message ?? 'Impossible de valider ce compte.')
    } finally {
      setEnCours(null)
    }
  }

  async function refuser(e, creator) {
    e.preventDefault()
    setError(null)
    setEnCours(creator.userId)
    try {
      await rejectCreator(creator.userId, refus.motif, token)
      retirer(creator.userId, `${creator.displayName} est refusé.`)
    } catch (err) {
      setError(err?.message ?? 'Impossible de refuser ce compte.')
    } finally {
      setEnCours(null)
    }
  }

  if (loading) return <main className={styles.container}><p>Chargement…</p></main>

  return (
    <main className={styles.container}>
      <h1>Back-office</h1>
      <h2>Comptes créateurs en attente de validation</h2>
      {error && <p role="alert" className={styles.error}>{error}</p>}
      {message && <p role="status">{message}</p>}

      {enAttente.length === 0 ? (
        <p className={styles.vide}>Aucun compte en attente.</p>
      ) : (
        <ul className={styles.liste} aria-label="Comptes en attente">
          {enAttente.map((c) => (
            <li key={c.userId}>
              <div>
                <strong>{c.displayName}</strong>
                <span className={styles.details}>{[c.city, c.country].filter(Boolean).join(', ') || 'Lieu non précisé'}</span>
                <p className={styles.bio}>{c.bio || 'Pas de présentation.'}</p>
              </div>

              {refus?.userId === c.userId ? (
                <form onSubmit={(e) => refuser(e, c)} className={styles.refus}>
                  <label>
                    Motif du refus
                    <textarea
                      value={refus.motif}
                      onChange={(e) => setRefus({ userId: c.userId, motif: e.target.value })}
                      maxLength={500}
                      required
                      autoFocus
                    />
                  </label>
                  <div className={styles.boutons}>
                    <button type="submit" disabled={enCours === c.userId}>Confirmer le refus</button>
                    <button type="button" onClick={() => setRefus(null)} disabled={enCours === c.userId}>Annuler</button>
                  </div>
                </form>
              ) : (
                <div className={styles.boutons}>
                  <button
                    type="button"
                    onClick={() => valider(c)}
                    disabled={enCours === c.userId}
                    aria-label={`Valider ${c.displayName}`}
                  >
                    Valider
                  </button>
                  <button
                    type="button"
                    onClick={() => setRefus({ userId: c.userId, motif: '' })}
                    disabled={enCours === c.userId}
                    aria-label={`Refuser ${c.displayName}`}
                  >
                    Refuser
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      <p><Link to="/">Voir la galerie</Link></p>
    </main>
  )
}
