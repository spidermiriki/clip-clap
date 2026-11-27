// Formats d'affichage partagés (prestations #27)

const EUROS = new Intl.NumberFormat('fr-BE', { style: 'currency', currency: 'EUR' })

/** 1200 → « 1 200,00 € » */
export function formatPrix(montant) {
  return EUROS.format(Number(montant))
}

/**
 * Lit un prix saisi à la main, en euros : « 1200 », « 89,90 », « 1 200,50 », « 1.200,50 » ou
 * « 1,200.50 ». Avec deux séparateurs, le dernier est la virgule décimale ; plusieurs points seuls
 * sont des séparateurs de milliers. Renvoie null si le format n'est pas reconnu.
 */
export function parsePrix(saisie) {
  // Espaces, y compris insécables (U+00A0, U+202F) que Intl.NumberFormat insère en fr-BE
  let texte = String(saisie ?? '').replace(/[\s  ]/g, '')
  const virgule = texte.lastIndexOf(',')
  const point = texte.lastIndexOf('.')
  if (virgule >= 0 && point >= 0) {
    const decimal = virgule > point ? ',' : '.'
    const milliers = decimal === ',' ? '.' : ','
    texte = texte.split(milliers).join('').replace(decimal, '.')
  } else if (virgule >= 0) {
    texte = texte.replace(',', '.')
  } else if (texte.split('.').length > 2) {
    texte = texte.split('.').join('')
  }
  return /^\d+(\.\d{1,2})?$/.test(texte) ? Number(texte) : null
}

/** 45 → « 45 min », 60 → « 1 h », 90 → « 1 h 30 » */
export function formatDuree(minutes) {
  const heures = Math.floor(minutes / 60)
  const reste = minutes % 60
  if (heures === 0) return `${reste} min`
  return reste === 0 ? `${heures} h` : `${heures} h ${String(reste).padStart(2, '0')}`
}

const JOUR = new Intl.DateTimeFormat('fr-BE', { timeZone: 'Europe/Brussels', weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
const HEURE = new Intl.DateTimeFormat('fr-BE', { timeZone: 'Europe/Brussels', hour: '2-digit', minute: '2-digit' })

/** Créneau à l'heure de Bruxelles (#32) : « vendredi 2 avril 2027, 14:00 – 22:00 » */
export function formatCreneau(start, end) {
  return `${JOUR.format(new Date(start))}, ${HEURE.format(new Date(start))} – ${HEURE.format(new Date(end))}`
}
