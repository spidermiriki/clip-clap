import { useEffect, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { Link } from 'react-router-dom'
import { createOffering, deleteOffering, listMyOfferings, updateOffering } from '../api/offerings'
import { formatDuree, formatPrix, parsePrix } from '../format'
import styles from './MesPrestations.module.css'

const FORMULAIRE_VIDE = { name: '', durationMinutes: '', basePrice: '', options: [] }


function versFormulaire(offering) {
  return {
    name: offering.name,
    durationMinutes: String(offering.durationMinutes),
    basePrice: String(offering.basePrice),
    options: offering.options.map((o) => ({ name: o.name, extraPrice: String(o.extraPrice) })),
  }
}

// Prestations à prix fixe du créateur connecté (wireframe 07, #27)
export default function MesPrestations() {
  const { user } = useAuth()
  const token = user?.access_token ?? null

  const [offerings, setOfferings] = useState([])
  const [loading, setLoading] = useState(true)
  const [sansProfil, setSansProfil] = useState(false)
  const [error, setError] = useState(null)
  const [formulaire, setFormulaire] = useState(FORMULAIRE_VIDE)
  // null : nouvelle prestation ; sinon l'id de la prestation modifiée
  const [editionId, setEditionId] = useState(null)
  const [envoi, setEnvoi] = useState(false)

  // Relancé à chaque nouveau token (renouvellement silencieux de la session) : on repart d'un état
  // propre, et une réponse arrivée après une plus récente est ignorée (revue de #119)
  useEffect(() => {
    let ignore = false
    setError(null)
    setSansProfil(false)
    listMyOfferings(token)
      .then((result) => {
        if (!ignore) setOfferings(result)
      })
      .catch((err) => {
        if (ignore) return
        if (err?.status === 404) setSansProfil(true)
        else setError(err?.message ?? 'Impossible de charger vos prestations.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [token])

  function champ(nom, valeur) {
    setFormulaire((f) => ({ ...f, [nom]: valeur }))
  }

  function champOption(index, nom, valeur) {
    setFormulaire((f) => ({ ...f, options: f.options.map((o, i) => (i === index ? { ...o, [nom]: valeur } : o)) }))
  }

  function ajouterOption() {
    setFormulaire((f) => ({ ...f, options: [...f.options, { name: '', extraPrice: '' }] }))
  }

  function retirerOption(index) {
    setFormulaire((f) => ({ ...f, options: f.options.filter((_, i) => i !== index) }))
  }

  function modifier(offering) {
    setError(null)
    setEditionId(offering.id)
    setFormulaire(versFormulaire(offering))
  }

  function annuler() {
    setEditionId(null)
    setFormulaire(FORMULAIRE_VIDE)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    const corps = {
      name: formulaire.name,
      durationMinutes: Number(formulaire.durationMinutes),
      basePrice: parsePrix(formulaire.basePrice),
      options: formulaire.options.map((o) => ({ name: o.name, extraPrice: parsePrix(o.extraPrice) })),
    }
    // Un prix illisible n'est jamais envoyé : le backend le prendrait pour un prix manquant
    if (corps.basePrice === null || corps.options.some((o) => o.extraPrice === null)) {
      setError('Format de prix invalide : utilisez par exemple 1200, 89,90 ou 1 200,50.')
      return
    }
    setEnvoi(true)
    try {
      if (editionId === null) {
        const creee = await createOffering(corps, token)
        setOfferings((prev) => [...prev, creee])
      } else {
        const modifiee = await updateOffering(editionId, corps, token)
        setOfferings((prev) => prev.map((o) => (o.id === editionId ? modifiee : o)))
      }
      annuler()
    } catch (err) {
      setError(err?.message ?? "Impossible d'enregistrer la prestation.")
    } finally {
      setEnvoi(false)
    }
  }

  async function supprimer(offering) {
    if (!window.confirm(`Supprimer la prestation « ${offering.name} » ?`)) return
    setError(null)
    try {
      await deleteOffering(offering.id, token)
      setOfferings((prev) => prev.filter((o) => o.id !== offering.id))
      if (editionId === offering.id) annuler()
    } catch (err) {
      setError(err?.message ?? 'Impossible de supprimer la prestation.')
    }
  }

  if (loading) return <main className={styles.container}><p>Chargement…</p></main>

  if (sansProfil) {
    return (
      <main className={styles.container}>
        <h1>Mes prestations</h1>
        <p>Créez d&apos;abord votre profil créateur pour pouvoir proposer des prestations.</p>
        <Link to="/mon-profil">Créer mon profil</Link>
      </main>
    )
  }

  return (
    <main className={styles.container}>
      <h1>Mes prestations</h1>
      {error && <p role="alert" className={styles.error}>{error}</p>}

      <div className={styles.layout}>
        <section aria-labelledby="titre-liste">
          <h2 id="titre-liste">Prestations proposées</h2>
          {offerings.length === 0 ? (
            <p className={styles.vide}>Aucune prestation pour le moment.</p>
          ) : (
            <ul className={styles.liste} aria-label="Mes prestations">
              {offerings.map((o) => (
                <li key={o.id}>
                  <div>
                    <strong>{o.name}</strong>
                    <span className={styles.details}>{formatDuree(o.durationMinutes)} · {formatPrix(o.basePrice)}</span>
                    {o.options.length > 0 && (
                      <ul className={styles.options}>
                        {o.options.map((opt) => (
                          <li key={opt.id}>{opt.name} : + {formatPrix(opt.extraPrice)}</li>
                        ))}
                      </ul>
                    )}
                  </div>
                  <div className={styles.boutons}>
                    <button type="button" onClick={() => modifier(o)} aria-label={`Modifier ${o.name}`}>Modifier</button>
                    <button type="button" onClick={() => supprimer(o)} aria-label={`Supprimer ${o.name}`}>Supprimer</button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <form onSubmit={handleSubmit} className={styles.formulaire} aria-labelledby="titre-formulaire">
          <h2 id="titre-formulaire">{editionId === null ? 'Nouvelle prestation' : 'Modifier la prestation'}</h2>
          <label>
            Nom
            <input value={formulaire.name} onChange={(e) => champ('name', e.target.value)} maxLength={100} required />
          </label>
          <label>
            Durée (minutes)
            <input
              type="number" min="1" max="1440" step="1" required
              value={formulaire.durationMinutes}
              onChange={(e) => champ('durationMinutes', e.target.value)}
            />
          </label>
          <label>
            Prix de base (€)
            <input
              inputMode="decimal" required
              value={formulaire.basePrice}
              onChange={(e) => champ('basePrice', e.target.value)}
            />
          </label>

          <fieldset className={styles.optionsSaisie}>
            <legend>Options payantes</legend>
            {formulaire.options.map((opt, index) => (
              <div key={index} className={styles.option}>
                <label>
                  Option {index + 1}
                  <input value={opt.name} onChange={(e) => champOption(index, 'name', e.target.value)} maxLength={100} required />
                </label>
                <label>
                  Supplément (€)
                  <input inputMode="decimal" value={opt.extraPrice} onChange={(e) => champOption(index, 'extraPrice', e.target.value)} required />
                </label>
                <button type="button" onClick={() => retirerOption(index)} aria-label={`Retirer l'option ${index + 1}`}>Retirer</button>
              </div>
            ))}
            <button type="button" onClick={ajouterOption}>+ Ajouter une option</button>
          </fieldset>

          <div className={styles.boutons}>
            <button type="submit" disabled={envoi}>Enregistrer</button>
            {editionId !== null && <button type="button" onClick={annuler}>Annuler</button>}
          </div>
        </form>
      </div>
    </main>
  )
}
