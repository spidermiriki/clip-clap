import { useEffect, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { acceptBooking, completeBooking, getCreatorBookings, refuseBooking } from '../api/creatorBookings'
import { getMyProfile } from '../api/creators'
import BandeauValidation from '../components/BandeauValidation'
import { formatCreneau, formatPrix } from '../format'
import styles from './TableauDeBord.module.css'

const STATUTS = {
  EN_ATTENTE: 'En attente',
  CONFIRMEE: 'Confirmée',
  REFUSEE: 'Refusée',
  ANNULEE: 'Annulée',
  EXPIREE: 'Expirée',
  TERMINEE: 'Terminée',
}

const POURCENTAGE = new Intl.NumberFormat('fr-BE', { style: 'percent' })

/**
 * Demandes acceptées parmi celles que le créateur a traitées ou laissé expirer. Les annulations
 * sont exclues : c'est le client qui annule. null s'il n'y a encore rien à compter.
 */
function tauxAcceptation(counts) {
  const acceptees = (counts.CONFIRMEE ?? 0) + (counts.TERMINEE ?? 0)
  const traitees = acceptees + (counts.REFUSEE ?? 0) + (counts.EXPIREE ?? 0)
  return traitees === 0 ? null : acceptees / traitees
}

/** « Marquer comme terminée » (#38) : réservation confirmée dont le créneau a commencé à l'heure du chargement. */
function peutTerminer(booking, chargeLe) {
  return booking.status === 'CONFIRMEE' && Boolean(booking.slot)
    && new Date(booking.slot.start).getTime() <= chargeLe
}

// Tableau de bord créateur (wireframe 08, #32) : demandes reçues, à accepter ou refuser (API de #31),
// et prestations commencées à marquer comme terminées (#38, API de #37)
export default function TableauDeBord() {
  const { user } = useAuth()
  const token = user?.access_token ?? null

  // Rechargé à chaque nouveau token et après chaque action (version) : une réponse arrivée après
  // une demande plus récente est ignorée, et les données précédentes restent affichées pendant le
  // rechargement
  const [version, setVersion] = useState(0)
  // chargeLe : heure du dernier chargement, qui décide si un créneau confirmé a commencé (#38)
  const [chargement, setChargement] = useState({ donnees: null, erreur: null, chargeLe: 0 })
  const [statut, setStatut] = useState(null)

  const [actionErreur, setActionErreur] = useState(null)
  const [enCours, setEnCours] = useState(null)
  // Réservation dont le formulaire de refus est ouvert, et son message
  const [refusId, setRefusId] = useState(null)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let ignore = false
    getCreatorBookings(token)
      .then((donnees) => {
        if (!ignore) setChargement({ donnees, erreur: null, chargeLe: Date.now() })
      })
      .catch((err) => {
        if (!ignore) {
          setChargement({ donnees: null, erreur: err?.message ?? 'Impossible de charger les réservations reçues.', chargeLe: Date.now() })
        }
      })
    return () => {
      ignore = true
    }
  }, [token, version])

  // Bandeau de validation (#33) : facultatif, le tableau de bord s'affiche sans lui
  useEffect(() => {
    let ignore = false
    getMyProfile(token)
      .then((profil) => {
        if (!ignore) setStatut(profil)
      })
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [token])

  async function agir(bookingId, action) {
    setActionErreur(null)
    setEnCours(bookingId)
    try {
      await action()
      setRefusId(null)
      setMessage('')
    } catch (err) {
      setActionErreur(err?.message ?? "L'action n'a pas pu être enregistrée.")
    } finally {
      setEnCours(null)
      setVersion((v) => v + 1)
    }
  }

  function ouvrirRefus(bookingId) {
    setRefusId(bookingId)
    setMessage('')
  }

  const { donnees, erreur, chargeLe } = chargement

  if (!donnees && !erreur) {
    return <main className={styles.container}><p>Chargement…</p></main>
  }

  const counts = donnees?.counts ?? {}
  const taux = tauxAcceptation(counts)

  return (
    <main className={styles.container}>
      <h1>Tableau de bord</h1>
      <BandeauValidation statut={statut} />
      {erreur && <p role="alert" className={styles.error}>{erreur}</p>}
      {actionErreur && <p role="alert" className={styles.error}>{actionErreur}</p>}

      {donnees && (
        <>
          <ul className={styles.compteurs} aria-label="Indicateurs">
            <li><span>En attente</span><strong>{counts.EN_ATTENTE ?? 0}</strong></li>
            <li><span>Confirmées</span><strong>{counts.CONFIRMEE ?? 0}</strong></li>
            <li><span>Taux d&apos;acceptation</span><strong>{taux === null ? '—' : POURCENTAGE.format(taux)}</strong></li>
            {/* Avis et notes : v0.3 */}
            <li><span>Note</span><strong>—</strong></li>
          </ul>

          <section aria-labelledby="titre-reservations">
            <h2 id="titre-reservations">Réservations reçues</h2>
            {donnees.bookings.length === 0 ? (
              <p>Vous n&apos;avez encore reçu aucune demande de réservation.</p>
            ) : (
              <ul className={styles.reservations}>
                {donnees.bookings.map((booking) => (
                  <li key={booking.id}>
                    <div>
                      <strong>{booking.offering?.name ?? 'Prestation supprimée'}</strong>
                      <span className={styles.details}>
                        {booking.slot ? formatCreneau(booking.slot.start, booking.slot.end) : 'Créneau supprimé'}
                      </span>
                      <span className={styles.details}>Client : {booking.client?.email}</span>
                      <span className={styles.details}>Total : {formatPrix(booking.totalPrice)}</span>
                      {booking.refusalMessage && (
                        <span className={styles.details}>Message de refus : {booking.refusalMessage}</span>
                      )}
                    </div>
                    <div className={styles.actions}>
                      <span className={styles.statut}>{STATUTS[booking.status] ?? booking.status}</span>
                      {booking.status === 'EN_ATTENTE' && refusId !== booking.id && (
                        <>
                          <button
                            type="button"
                            disabled={enCours !== null}
                            onClick={() => agir(booking.id, () => acceptBooking(booking.id, token))}
                          >
                            Accepter
                          </button>
                          <button type="button" disabled={enCours !== null} onClick={() => ouvrirRefus(booking.id)}>
                            Refuser
                          </button>
                        </>
                      )}
                      {peutTerminer(booking, chargeLe) && (
                        <button
                          type="button"
                          disabled={enCours !== null}
                          onClick={() => agir(booking.id, () => completeBooking(booking.id, token))}
                        >
                          Marquer comme terminée
                        </button>
                      )}
                    </div>
                    {booking.status === 'EN_ATTENTE' && refusId === booking.id && (
                      <form
                        className={styles.refus}
                        onSubmit={(e) => {
                          e.preventDefault()
                          agir(booking.id, () => refuseBooking(booking.id, message.trim(), token))
                        }}
                      >
                        <label>
                          Message au client (facultatif)
                          <textarea maxLength={500} value={message} onChange={(e) => setMessage(e.target.value)} />
                        </label>
                        <div className={styles.boutons}>
                          <button type="submit" disabled={enCours !== null}>Confirmer le refus</button>
                          <button type="button" onClick={() => setRefusId(null)}>Annuler</button>
                        </div>
                      </form>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </main>
  )
}
