import { useEffect, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { Link } from 'react-router-dom'
import { createSlot, deleteSlot, getMySettings, listMySlots, saveMySettings } from '../api/availability'
import styles from './MesDisponibilites.module.css'

const JOUR = new Intl.DateTimeFormat('fr-BE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

// Les dates du backend portent déjà l'heure de Bruxelles (« 2027-04-02T14:00:00+02:00 ») :
// on affiche leur partie locale telle quelle, quel que soit le fuseau du navigateur
const jourDe = (date) => date.slice(0, 10)
const heureDe = (date) => date.slice(11, 16)
const libelleJour = (jour) => JOUR.format(new Date(`${jour}T12:00:00`))

function parJour(slots) {
  const groupes = new Map()
  for (const slot of [...slots].sort((a, b) => Date.parse(a.start) - Date.parse(b.start))) {
    const jour = jourDe(slot.start)
    groupes.set(jour, [...(groupes.get(jour) ?? []), slot])
  }
  return [...groupes.entries()]
}

// Créneaux libres, préavis et marge du créateur connecté (wireframe 07, #28)
export default function MesDisponibilites() {
  const { user } = useAuth()
  const token = user?.access_token ?? null

  const [slots, setSlots] = useState([])
  const [reglages, setReglages] = useState({ minNoticeHours: '0', bufferMinutes: '0' })
  const [loading, setLoading] = useState(true)
  const [sansProfil, setSansProfil] = useState(false)
  const [error, setError] = useState(null)
  const [message, setMessage] = useState(null)
  const [creneau, setCreneau] = useState({ date: '', debut: '', fin: '' })
  const [envoi, setEnvoi] = useState(false)

  useEffect(() => {
    Promise.all([listMySlots(token), getMySettings(token)])
      .then(([creneaux, settings]) => {
        setSlots(creneaux)
        setReglages({ minNoticeHours: String(settings.minNoticeHours), bufferMinutes: String(settings.bufferMinutes) })
      })
      .catch((err) => {
        if (err?.status === 404) setSansProfil(true)
        else setError(err?.message ?? 'Impossible de charger vos disponibilités.')
      })
      .finally(() => setLoading(false))
  }, [token])

  async function enregistrerReglages(e) {
    e.preventDefault()
    setError(null)
    setMessage(null)
    try {
      const enregistres = await saveMySettings(
        { minNoticeHours: Number(reglages.minNoticeHours), bufferMinutes: Number(reglages.bufferMinutes) },
        token,
      )
      setReglages({ minNoticeHours: String(enregistres.minNoticeHours), bufferMinutes: String(enregistres.bufferMinutes) })
      setMessage('Réglages enregistrés.')
    } catch (err) {
      setError(err?.message ?? "Impossible d'enregistrer vos réglages.")
    }
  }

  async function ajouter(e) {
    e.preventDefault()
    setError(null)
    setMessage(null)
    setEnvoi(true)
    try {
      const cree = await createSlot(`${creneau.date}T${creneau.debut}`, `${creneau.date}T${creneau.fin}`, token)
      setSlots((prev) => [...prev, cree])
      setCreneau((c) => ({ ...c, debut: '', fin: '' }))
    } catch (err) {
      setError(err?.message ?? "Impossible d'ajouter le créneau.")
    } finally {
      setEnvoi(false)
    }
  }

  async function supprimer(slot) {
    const quand = `${libelleJour(jourDe(slot.start))}, ${heureDe(slot.start)}–${heureDe(slot.end)}`
    if (!window.confirm(`Supprimer le créneau du ${quand} ?`)) return
    setError(null)
    setMessage(null)
    try {
      await deleteSlot(slot.id, token)
      setSlots((prev) => prev.filter((s) => s.id !== slot.id))
    } catch (err) {
      setError(err?.message ?? 'Impossible de supprimer le créneau.')
    }
  }

  if (loading) return <main className={styles.container}><p>Chargement…</p></main>

  if (sansProfil) {
    return (
      <main className={styles.container}>
        <h1>Mes disponibilités</h1>
        <p>Créez d&apos;abord votre profil créateur pour pouvoir proposer des créneaux.</p>
        <Link to="/mon-profil">Créer mon profil</Link>
      </main>
    )
  }

  return (
    <main className={styles.container}>
      <h1>Mes disponibilités</h1>
      <p className={styles.aide}>Heures de Bruxelles. Les clients ne voient que les créneaux qui respectent votre préavis et votre marge.</p>
      {error && <p role="alert" className={styles.error}>{error}</p>}
      {message && <p role="status">{message}</p>}

      <div className={styles.layout}>
        <section aria-labelledby="titre-creneaux">
          <h2 id="titre-creneaux">Créneaux libres</h2>
          {slots.length === 0 ? (
            <p className={styles.vide}>Aucun créneau pour les 30 prochains jours.</p>
          ) : (
            parJour(slots).map(([jour, duJour]) => (
              <div key={jour} className={styles.jour}>
                <h3>{libelleJour(jour)}</h3>
                <ul aria-label={`Créneaux du ${libelleJour(jour)}`}>
                  {duJour.map((slot) => (
                    <li key={slot.id}>
                      <span>{heureDe(slot.start)} – {heureDe(slot.end)}</span>
                      <button
                        type="button"
                        onClick={() => supprimer(slot)}
                        aria-label={`Supprimer le créneau de ${heureDe(slot.start)} le ${libelleJour(jour)}`}
                      >
                        Supprimer
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ))
          )}
        </section>

        <div className={styles.formulaires}>
          <form onSubmit={ajouter} className={styles.formulaire} aria-labelledby="titre-ajout">
            <h2 id="titre-ajout">Ajouter un créneau libre</h2>
            <label>
              Date
              <input type="date" required value={creneau.date} onChange={(e) => setCreneau((c) => ({ ...c, date: e.target.value }))} />
            </label>
            <label>
              Début
              <input type="time" required value={creneau.debut} onChange={(e) => setCreneau((c) => ({ ...c, debut: e.target.value }))} />
            </label>
            <label>
              Fin
              <input type="time" required value={creneau.fin} onChange={(e) => setCreneau((c) => ({ ...c, fin: e.target.value }))} />
            </label>
            <button type="submit" disabled={envoi}>Ajouter</button>
          </form>

          <form onSubmit={enregistrerReglages} className={styles.formulaire} aria-labelledby="titre-reglages">
            <h2 id="titre-reglages">Préavis et marge</h2>
            <label>
              Préavis minimum (heures)
              <input
                type="number" min="0" max="720" step="1" required
                value={reglages.minNoticeHours}
                onChange={(e) => setReglages((r) => ({ ...r, minNoticeHours: e.target.value }))}
              />
            </label>
            <label>
              Marge entre deux rendez-vous (minutes)
              <input
                type="number" min="0" max="480" step="5" required
                value={reglages.bufferMinutes}
                onChange={(e) => setReglages((r) => ({ ...r, bufferMinutes: e.target.value }))}
              />
            </label>
            <button type="submit">Enregistrer les réglages</button>
          </form>
        </div>
      </div>
    </main>
  )
}
