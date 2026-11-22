import { useEffect, useRef, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { Link, Outlet } from 'react-router-dom'
import { getJson } from './api'
import './App.css'
import { roleFromProfile } from './auth/role'

// Mise en page commune : navigation et connexion/déconnexion (#11). Le design complet des
// écrans et la protection des routes par rôle arrivent avec #16.
function App() {
  const auth = useAuth()

  // Compte local (#97) : GET /api/users/me crée la ligne `users` si le token porte déjà un rôle.
  // Un seul appel par utilisateur connecté (clé : le sub), même si le token est renouvelé.
  const sub = auth.user?.profile?.sub
  const token = auth.user?.access_token
  const aSynchroniser = auth.isAuthenticated && Boolean(sub) && Boolean(token)
  const subDemande = useRef(null)
  const [subSynchronise, setSubSynchronise] = useState(null)

  useEffect(() => {
    if (!aSynchroniser || subDemande.current === sub) return
    subDemande.current = sub
    getJson('/api/users/me', token)
      .catch((err) => console.error('Création du compte local impossible (GET /api/users/me) :', err))
      .finally(() => setSubSynchronise(sub))
  }, [aSynchroniser, sub, token])

  // Les pages attendent la fin de l'appel (réussi ou non) : sinon « Mon profil » pourrait
  // interroger le backend avant la création du compte local et recevoir un 404
  const compteEnCours = aSynchroniser && subSynchronise !== sub

  if (auth.isLoading) {
    return <main className="auth-page"><p>Chargement…</p></main>
  }

  if (auth.error) {
    return (
      <main className="auth-page">
        <h1>Connexion impossible</h1>
        <p className="auth-error">{auth.error.message}</p>
        <button type="button" onClick={() => auth.signinRedirect()}>Réessayer</button>
      </main>
    )
  }

  return (
    <>
      <nav className="app-nav">
        <Link to="/">Galerie</Link>
        {auth.isAuthenticated ? (
          <>
            {roleFromProfile(auth.user?.profile) === 'CREATOR' && (
              <>
                <Link to="/mon-profil">Mon profil</Link>
                <Link to="/mes-albums">Mes albums</Link>
                <Link to="/mes-prestations">Mes prestations</Link>
              </>
            )}
            {roleFromProfile(auth.user?.profile) === 'ADMIN' && <Link to="/admin">Back-office</Link>}
            <span>{auth.user?.profile.email}</span>
            <button type="button" className="link" onClick={() => auth.signoutRedirect()}>
              Se déconnecter
            </button>
          </>
        ) : (
          <>
            <button type="button" className="link" onClick={() => auth.signinRedirect()}>
              Connexion
            </button>
            <Link to="/inscription">Inscription</Link>
          </>
        )}
      </nav>
      {compteEnCours ? <main className="auth-page"><p>Chargement…</p></main> : <Outlet />}
    </>
  )
}

export default App
