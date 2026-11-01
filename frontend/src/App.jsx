import { useEffect, useState } from 'react'
import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { useAuth } from 'react-oidc-context'
import MonProfilCreateur from './pages/MonProfilCreateur'
import './App.css'

// TODO #16 (Gedeon) : navigation complète et routes protégées/publiques par rôle.
// TODO #13 : /galerie (galerie publique) devra être accessible sans connexion.
function App() {
  const auth = useAuth()
  const [account, setAccount] = useState(null)
  const [apiError, setApiError] = useState(null)

  const accessToken = auth.user?.access_token

  useEffect(() => {
    if (!accessToken) return

    // Le backend reconnaît l'utilisateur grâce au token et crée son compte à la première connexion
    fetch('/api/users/me', { headers: { Authorization: `Bearer ${accessToken}` } })
      .then((response) => {
        if (!response.ok) throw new Error(`Erreur ${response.status}`)
        return response.json()
      })
      .then(setAccount)
      .catch((error) => setApiError(error.message))
  }, [accessToken])

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

  if (!auth.isAuthenticated) {
    return (
      <main className="auth-page">
        <h1>Clic &amp; Clap</h1>
        <p>Connectez-vous pour réserver ou gérer votre profil.</p>
        <button type="button" onClick={() => auth.signinRedirect()}>Se connecter</button>
        <p className="auth-hint">
          Pas de compte ?{' '}
          <button type="button" className="link" onClick={() => auth.signinRedirect({ prompt: 'create' })}>
            S'inscrire
          </button>
        </p>
      </main>
    )
  }

  return (
    <BrowserRouter>
      <Routes>
        {/* Page provisoire (#10) — design et navigation arrivent avec #16 (Gedeon). */}
        <Route path="/" element={
          <main className="auth-page">
            <h1>Bonjour {account?.email ?? auth.user?.profile.email}</h1>
            {account && <p>Compte n° {account.id} · rôle {account.role}</p>}
            {apiError && <p className="auth-error">Le backend n'a pas reconnu la session ({apiError}).</p>}
            <button type="button" onClick={() => auth.signoutRedirect()}>Se déconnecter</button>
            <Link to="/mon-profil-createur">Mon profil créateur</Link>
          </main>
        } />
        <Route path="/mon-profil-createur" element={<MonProfilCreateur />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
