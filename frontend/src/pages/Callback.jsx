import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from 'react-oidc-context'
import { postJson } from '../api'
import { roleFromProfile, routeForRole } from '../auth/role'

// Retour de Zitadel après connexion ou inscription (#11). Si l'inscrit vient de choisir un
// rôle sur /inscription (porté dans `auth.user.state`) et que le token n'en porte encore
// aucun, on l'attribue côté backend puis on rafraîchit le token : c'est ce nouveau token,
// émis par Zitadel (source de vérité des rôles), qui détermine la redirection finale.
// Une connexion normale (sans inscription) redirige directement selon le rôle déjà présent.
function Callback() {
  const auth = useAuth()
  const navigate = useNavigate()
  const dejaTraite = useRef(false)

  useEffect(() => {
    if (!auth.isAuthenticated || dejaTraite.current) return
    dejaTraite.current = true

    const attribuerRoleSiNecessaire = async () => {
      const roleChoisi = auth.user?.state?.role
      const roleActuel = roleFromProfile(auth.user?.profile)
      let roleFinal = roleActuel

      if (!roleActuel && roleChoisi) {
        await postJson('/api/users/me/role', { role: roleChoisi }, auth.user.access_token)
        const utilisateurRafraichi = await auth.signinSilent()
        roleFinal = roleFromProfile(utilisateurRafraichi?.profile) ?? roleChoisi
      }

      navigate(routeForRole(roleFinal), { replace: true })
    }

    attribuerRoleSiNecessaire().catch(() => navigate('/', { replace: true }))
  }, [auth, navigate])

  return <main className="auth-page"><p>Connexion en cours…</p></main>
}

export default Callback
