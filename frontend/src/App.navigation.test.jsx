import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

let mockAuth

vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))
// App peut appeler GET /api/users/me à la connexion (#97) : aucun appel réseau dans ces tests
vi.mock('./api', () => ({ getJson: vi.fn().mockResolvedValue({}), postJson: vi.fn() }))

const CLAIM_ROLES = 'urn:zitadel:iam:org:project:roles'

function connecteAvecRole(role) {
  return {
    isLoading: false,
    error: null,
    isAuthenticated: true,
    user: { access_token: 'jeton', profile: { sub: `sub-${role}`, email: 'compte@exemple.test', [CLAIM_ROLES]: { [role]: {} } } },
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
  }
}

function renderApp() {
  return render(
    <MemoryRouter>
      <Routes>
        <Route element={<App />}>
          <Route index element={<p>Galerie</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('App — navigation selon le rôle', () => {
  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(cleanup)

  it('un créateur voit « Mon profil » et « Mes albums »', async () => {
    mockAuth = connecteAvecRole('CREATOR')
    renderApp()

    expect(await screen.findByRole('link', { name: 'Mon profil' })).toHaveAttribute('href', '/mon-profil')
    expect(screen.getByRole('link', { name: 'Mes albums' })).toHaveAttribute('href', '/mes-albums')
  })

  it('un client ne voit pas les liens réservés aux créateurs', async () => {
    mockAuth = connecteAvecRole('CLIENT')
    renderApp()

    expect(await screen.findByRole('link', { name: 'Galerie' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Mon profil' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Mes albums' })).not.toBeInTheDocument()
  })

  it('un visiteur non connecté ne voit que Galerie, Connexion et Inscription', () => {
    mockAuth = { isLoading: false, error: null, isAuthenticated: false, user: null, signinRedirect: vi.fn() }
    renderApp()

    expect(screen.queryByRole('link', { name: 'Mon profil' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Inscription' })).toHaveAttribute('href', '/inscription')
  })
})
