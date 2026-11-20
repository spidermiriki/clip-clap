// Formats d'affichage partagés (prestations #27)

const EUROS = new Intl.NumberFormat('fr-BE', { style: 'currency', currency: 'EUR' })

/** 1200 → « 1 200,00 € » */
export function formatPrix(montant) {
  return EUROS.format(Number(montant))
}

/** 45 → « 45 min », 60 → « 1 h », 90 → « 1 h 30 » */
export function formatDuree(minutes) {
  const heures = Math.floor(minutes / 60)
  const reste = minutes % 60
  if (heures === 0) return `${reste} min`
  return reste === 0 ? `${heures} h` : `${heures} h ${String(reste).padStart(2, '0')}`
}
