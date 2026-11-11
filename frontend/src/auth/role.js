const CLAIM_GENERIQUE = 'urn:zitadel:iam:org:project:roles'
const CLAIM_PAR_PROJET = /^urn:zitadel:iam:org:project:.+:roles$/

// Lit le rôle (CLIENT/CREATOR/ADMIN) depuis le claim de rôles Zitadel du profil OIDC (#11).
// Partagé entre Callback.jsx (attribution du rôle à l'inscription) et ProtectedRoute (#16).
export function roleFromProfile(profile) {
  const entry = Object.entries(profile ?? {}).find(
    ([cle]) => cle === CLAIM_GENERIQUE || CLAIM_PAR_PROJET.test(cle),
  )
  if (!entry) return null
  const roles = Object.keys(entry[1] ?? {})
  if (roles.includes('ADMIN')) return 'ADMIN'
  if (roles.includes('CREATOR')) return 'CREATOR'
  if (roles.includes('CLIENT')) return 'CLIENT'
  return null
}

export function routeForRole(role) {
  return role === 'CREATOR' ? '/mon-profil' : '/'
}
