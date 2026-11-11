import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProtectedRoute from './ProtectedRoute'

const signinRedirect = vi.fn()
let mockAuth

vi.mock('react-oidc-context', () => ({
  useAuth: () => mockAuth,
}))

function renderMonProfil() {
  return render(
    <MemoryRouter initialEntries={['/mon-profil']}>
      <Routes>
        <Route path="/" element={<p>Galerie</p>} />
        <Route
          path="/mon-profil"
          element={(
            <ProtectedRoute roles={['CREATOR']}>
              <p>Profil créateur</p>
            </ProtectedRoute>
          )}
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    signinRedirect.mockClear()
  })

  it('redirige vers la connexion si non authentifié (AC1)', () => {
    mockAuth = { isAuthenticated: false, user: null, signinRedirect }

    renderMonProfil()

    expect(signinRedirect).toHaveBeenCalledOnce()
    expect(screen.queryByText('Profil créateur')).not.toBeInTheDocument()
  })

  it('redirige vers la page du rôle si le rôle ne correspond pas (AC2)', () => {
    mockAuth = {
      isAuthenticated: true,
      signinRedirect,
      user: { profile: { 'urn:zitadel:iam:org:project:roles': { CLIENT: {} } } },
    }

    renderMonProfil()

    expect(screen.getByText('Galerie')).toBeInTheDocument()
    expect(screen.queryByText('Profil créateur')).not.toBeInTheDocument()
    expect(signinRedirect).not.toHaveBeenCalled()
  })

  it('affiche la page si le rôle correspond', () => {
    mockAuth = {
      isAuthenticated: true,
      signinRedirect,
      user: { profile: { 'urn:zitadel:iam:org:project:roles': { CREATOR: {} } } },
    }

    renderMonProfil()

    expect(screen.getByText('Profil créateur')).toBeInTheDocument()
  })
})
