import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from 'react-oidc-context'
import { getCreatorProfile } from '../api/catalog'
import { getAvailableSlots } from '../api/availability'
import { createBooking } from '../api/bookings'
import { formatDuree, formatPrix } from '../format'
import styles from './Reservation.module.css'

const heureDe = (date) => date.slice(11, 16)

// Réservation d'un créneau chez un créateur (wireframe 05, #29)
export default function Reservation() {
  const { userId } = useParams()
  const { user } = useAuth()
  const token = user?.access_token ?? null

  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(null)

  const [offeringId, setOfferingId] = useState('')
  const [date, setDate] = useState('')
  const [slots, setSlots] = useState([])
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [slotId, setSlotId] = useState('')
  const [optionIds, setOptionIds] = useState(new Set())

  const [envoi, setEnvoi] = useState(false)
  const [error, setError] = useState(null)
  const [reservation, setReservation] = useState(null)

  useEffect(() => {
    let ignore = false
    getCreatorProfile(userId)
      .then((result) => {
        if (!ignore) setProfile(result)
      })
      .catch((err) => {
        if (!ignore) {
          setLoadError(err?.status === 404
            ? "Ce créateur n'existe pas ou n'est plus visible."
            : 'Impossible de charger ce profil.')
        }
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [userId])

  const offering = profile?.offerings?.find((o) => String(o.id) === offeringId) ?? null

  // Nouvelle liste de créneaux à chaque changement de prestation ou de date
  useEffect(() => {
    setSlotId('')
    setSlots([])
    if (!offeringId || !date) return
    let ignore = false
    setSlotsLoading(true)
    getAvailableSlots(userId, date, offeringId)
      .then((result) => {
        if (!ignore) setSlots(result)
      })
      .catch(() => {
        if (!ignore) setSlots([])
      })
      .finally(() => {
        if (!ignore) setSlotsLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [userId, offeringId, date])

  function toggleOption(id) {
    setOptionIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const total = offering
    ? offering.options
        .filter((o) => optionIds.has(o.id))
        .reduce((somme, o) => somme + Number(o.extraPrice), Number(offering.basePrice))
    : 0

  async function envoyer(e) {
    e.preventDefault()
    setError(null)
    setEnvoi(true)
    try {
      const cree = await createBooking(
        { offeringId: Number(offeringId), slotId: Number(slotId), optionIds: [...optionIds] },
        token,
      )
      setReservation(cree)
    } catch (err) {
      setError(err?.message ?? "Impossible d'envoyer la demande de réservation.")
      if (err?.status === 409) {
        // Le créneau choisi vient d'être pris : on retire le choix et on recharge la liste
        setSlotId('')
        getAvailableSlots(userId, date, offeringId).then(setSlots).catch(() => {})
      }
    } finally {
      setEnvoi(false)
    }
  }

  if (loading) return <main className={styles.container}><p>Chargement…</p></main>

  if (loadError) {
    return (
      <main className={styles.container}>
        <p role="alert" className={styles.error}>{loadError}</p>
        <Link to="/">Retour à la galerie</Link>
      </main>
    )
  }

  if (reservation) {
    return (
      <main className={styles.container}>
        <h1>Réserver chez {profile.displayName}</h1>
        <p className={styles.statut}>Statut : En attente</p>
        <p>Votre demande a été envoyée à {profile.displayName}. Vous serez informé·e dès qu&apos;elle sera traitée.</p>
        <Link to={`/createurs/${userId}`}>Retour au profil</Link>
      </main>
    )
  }

  return (
    <main className={styles.container}>
      <Link to={`/createurs/${userId}`} className={styles.retour}>← Profil de {profile.displayName}</Link>
      <h1>Réserver chez {profile.displayName}</h1>
      {error && <p role="alert" className={styles.error}>{error}</p>}

      <form onSubmit={envoyer} className={styles.formulaire}>
        <section aria-labelledby="titre-prestation">
          <h2 id="titre-prestation">1. Choix de la prestation</h2>
          <select
            required
            value={offeringId}
            onChange={(e) => {
              setOfferingId(e.target.value)
              setOptionIds(new Set())
            }}
          >
            <option value="" disabled>Choisissez une prestation</option>
            {profile.offerings?.map((o) => (
              <option key={o.id} value={o.id}>
                {o.name} — {formatDuree(o.durationMinutes)} — {formatPrix(o.basePrice)}
              </option>
            ))}
          </select>
        </section>

        <section aria-labelledby="titre-creneau">
          <h2 id="titre-creneau">2. Calendrier des créneaux libres</h2>
          <label>
            Date
            <input type="date" required value={date} onChange={(e) => setDate(e.target.value)} />
          </label>
          {slotsLoading && <p>Chargement des créneaux…</p>}
          {!slotsLoading && date && offeringId && slots.length === 0 && (
            <p className={styles.vide}>Aucun créneau disponible ce jour-là.</p>
          )}
          {slots.length > 0 && (
            <ul className={styles.creneaux} aria-label="Créneaux disponibles">
              {slots.map((slot) => (
                <li key={slot.id}>
                  <button
                    type="button"
                    aria-pressed={String(slot.id) === slotId}
                    className={String(slot.id) === slotId ? styles.creneauChoisi : undefined}
                    onClick={() => setSlotId(String(slot.id))}
                  >
                    {heureDe(slot.start)} – {heureDe(slot.end)}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {offering && offering.options.length > 0 && (
          <section aria-labelledby="titre-options">
            <h2 id="titre-options">3. Options payantes</h2>
            <ul className={styles.options}>
              {offering.options.map((o) => (
                <li key={o.id}>
                  <label>
                    <input type="checkbox" checked={optionIds.has(o.id)} onChange={() => toggleOption(o.id)} />
                    {o.name} : + {formatPrix(o.extraPrice)}
                  </label>
                </li>
              ))}
            </ul>
          </section>
        )}

        <section aria-labelledby="titre-recap" className={styles.recap}>
          <h2 id="titre-recap">Récapitulatif</h2>
          <p className={styles.total}>Total : {formatPrix(total)}</p>
          <button type="submit" disabled={!offeringId || !slotId || envoi}>Envoyer</button>
        </section>
      </form>
    </main>
  )
}
