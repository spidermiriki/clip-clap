import { useState } from 'react'
import { useAuth } from 'react-oidc-context'
import '../App.css'

const ROLES = [
  { value: 'CLIENT', label: 'Client' },
  { value: 'CREATOR', label: 'Créateur' },
]

// Écran d'inscription (wireframe 03, #11) : le visiteur choisit son rôle avant d'être
// redirigé vers l'inscription Zitadel. Zitadel ne connaît que l'identité, pas notre rôle
// métier, donc on le transporte nous-mêmes dans le `state` OIDC : il revient dans
// `auth.user.state` au retour sur /callback, où il est attribué côté backend.
function Inscription() {
  const auth = useAuth()
  const [role, setRole] = useState(null)

  return (
    <main className="auth-page">
      <h1>Créer un compte</h1>
      <p>Je suis :</p>
      <div className="role-choice">
        {ROLES.map(({ value, label }) => (
          <label key={value} className={`role-card${role === value ? ' role-card--selected' : ''}`}>
            <input
              type="radio"
              name="role"
              value={value}
              checked={role === value}
              onChange={() => setRole(value)}
            />
            {label}
          </label>
        ))}
      </div>
      <button
        type="button"
        disabled={!role}
        onClick={() => auth.signinRedirect({ prompt: 'create', state: { role } })}
      >
        Continuer
      </button>
    </main>
  )
}

export default Inscription
