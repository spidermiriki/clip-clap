import styles from './BandeauValidation.module.css'

// Statut de validation du profil par l'administrateur (#33) : rien à signaler une fois validé.
// Partagé entre « Mon profil » et le tableau de bord créateur (#32).
export default function BandeauValidation({ statut }) {
  if (statut?.validationStatus === 'PENDING') {
    return (
      <p role="status" className={styles.bandeau}>
        Votre profil est en attente de validation par un administrateur : il n&apos;apparaîtra dans la
        galerie qu&apos;une fois validé. Vous pouvez déjà préparer vos albums et vos prestations.
      </p>
    )
  }
  if (statut?.validationStatus === 'REJECTED') {
    return (
      <p role="status" className={`${styles.bandeau} ${styles.refuse}`}>
        Votre profil a été refusé : {statut.rejectionReason}. Corrigez-le puis enregistrez : il repassera en
        attente de validation.
      </p>
    )
  }
  return null
}
