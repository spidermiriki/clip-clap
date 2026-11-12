import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getJson } from './api'
import App from './App'

let mockAuth

vi.mock('react-oidc-context', () => ({ useAuth: () => mockAuth }))
vi.mock('./api', () => ({ getJson: vi.fn() }))

function connecte(sub, accessToken) {
  return {
    isLoading: false,
    error: null,
    isAuthenticated: true,
    user: { access_token: accessToken, profile: { sub, email: `${sub}@exemple.test` } },
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
  }
}

const VISITEUR = { isLoading: false, error: null, isAuthenticated: false, user: null, signinRedirect: vi.fn() }

function arbre() {
  return (
    <MemoryRouter>
      <Routes>
        <Route element={<App />}>
          <Route index element={<p>Contenu de la page</p>} />
        </Route>
      </Routes>
    </MemoryRouter>
  )
}

describe('App — compte local à la connexion (#97)', () => {
  beforeEach(() => {
    vi.mocked(getJson).mockReset().mockResolvedValue({ id: 1, role: 'CREATOR' })
  })

  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(cleanup)

  it('appelle GET /api/users/me une fois après la connexion, puis affiche la page', async () => {
    let terminer
    vi.mocked(getJson).mockReturnValue(new Promise((resolve) => { terminer = resolve }))
    mockAuth = connecte('sub-1', 'jeton-1')

    render(arbre())

    expect(getJson).toHaveBeenCalledTimes(1)
    expect(getJson).toHaveBeenCalledWith('/api/users/me', 'jeton-1')
    expect(screen.queryByText('Contenu de la page')).not.toBeInTheDocument()

    terminer({ id: 1, role: 'CREATOR' })
    expect(await screen.findByText('Contenu de la page')).toBeInTheDocument()
  })

  it('n\'appelle rien pour un visiteur non connecté', () => {
    mockAuth = VISITEUR

    render(arbre())

    expect(getJson).not.toHaveBeenCalled()
    expect(screen.getByText('Contenu de la page')).toBeInTheDocument()
  })

  it('ne rappelle pas si le composant se re-rend, ni quand le token est renouvelé', async () => {
    mockAuth = connecte('sub-1', 'jeton-1')
    const { rerender } = render(arbre())
    await screen.findByText('Contenu de la page')

    rerender(arbre())
    mockAuth = connecte('sub-1', 'jeton-renouvele')
    rerender(arbre())

    expect(await screen.findByText('Contenu de la page')).toBeInTheDocument()
    expect(getJson).toHaveBeenCalledTimes(1)
  })

  it('rappelle pour un autre utilisateur connecté dans le même navigateur', async () => {
    mockAuth = connecte('sub-1', 'jeton-1')
    const { rerender } = render(arbre())
    await screen.findByText('Contenu de la page')

    mockAuth = connecte('sub-2', 'jeton-2')
    rerender(arbre())

    await screen.findByText('Contenu de la page')
    expect(getJson).toHaveBeenCalledTimes(2)
    expect(getJson).toHaveBeenLastCalledWith('/api/users/me', 'jeton-2')
  })

  it('en cas d\'échec : la page s\'affiche quand même et l\'erreur part dans la console', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.mocked(getJson).mockRejectedValue(new Error('Erreur 500'))
    mockAuth = connecte('sub-1', 'jeton-1')

    render(arbre())

    expect(await screen.findByText('Contenu de la page')).toBeInTheDocument()
    expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('/api/users/me'), expect.any(Error))
    consoleError.mockRestore()
  })
})
