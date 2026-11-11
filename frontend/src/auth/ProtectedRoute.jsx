import { useEffect, useRef } from 'react'
import { useAuth } from 'react-oidc-context'
import { Navigate } from 'react-router-dom'
import { roleFromProfile, routeForRole } from './role'

// Garde de route (#16) : à utiliser sous <App/>, qui a déjà géré le chargement et les
// erreurs de connexion Zitadel avant de rendre l'Outlet - ici auth.isLoading est donc
// toujours false. `roles` est la liste des rôles autorisés (omis = juste être connecté).
function ProtectedRoute({ roles, children }) {
  const auth = useAuth()
  const redirectionDemandee = useRef(false)

  useEffect(() => {
    if (!auth.isAuthenticated && !redirectionDemandee.current) {
      redirectionDemandee.current = true
      auth.signinRedirect()
    }
  }, [auth])

  if (!auth.isAuthenticated) {
    return <main className="auth-page"><p>Redirection vers la connexion…</p></main>
  }

  const role = roleFromProfile(auth.user?.profile)
  if (roles && !roles.includes(role)) {
    return <Navigate to={routeForRole(role)} replace />
  }

  return children
}

export default ProtectedRoute
