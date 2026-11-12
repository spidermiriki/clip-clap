import { useAuth } from 'react-oidc-context'
import { Link, Outlet } from 'react-router-dom'
import './App.css'
import { roleFromProfile } from './auth/role'

// Mise en page commune : navigation et connexion/déconnexion (#11). Le design complet des
// écrans et la protection des routes par rôle arrivent avec #16.
function App() {
  const auth = useAuth()

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
            {roleFromProfile(auth.user?.profile) === 'CREATOR' && <Link to="/mes-albums">Mes albums</Link>}
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
      <Outlet />
    </>
  )
}

export default App
