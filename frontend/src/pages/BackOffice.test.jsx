import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { listPendingCreators, rejectCreator, validateCreator } from '../api/admin'
import BackOffice from './BackOffice'

// Token modifiable : simule le renouvellement silencieux de la session (#129)
const auth = vi.hoisted(() => ({ token: 'jeton' }))
vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: auth.token } }) }))
vi.mock('../api/admin', () => ({
  listPendingCreators: vi.fn(),
  validateCreator: vi.fn(),
  rejectCreator: vi.fn(),
}))

const CLARA = { userId: 3, displayName: 'Clara Lecomte', city: 'Mons', country: 'Belgique', bio: 'Mariages.' }

const page = () => <MemoryRouter><BackOffice /></MemoryRouter>

function renderPage() {
  return render(page())
}

describe('BackOffice (#33)', () => {
  beforeEach(() => {
    auth.token = 'jeton'
    vi.mocked(listPendingCreators).mockReset().mockResolvedValue([CLARA])
    vi.mocked(validateCreator).mockReset().mockResolvedValue({ ...CLARA, validationStatus: 'VALIDATED' })
    vi.mocked(rejectCreator).mockReset().mockResolvedValue({ ...CLARA, validationStatus: 'REJECTED' })
  })

  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(cleanup)

  it('valide un compte en attente (scénario 23)', async () => {
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Valider Clara Lecomte' }))

    expect(await screen.findByText('Aucun compte en attente.')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Clara Lecomte est validé')
    expect(validateCreator).toHaveBeenCalledWith(3, 'jeton')
  })

  it('refuse un compte avec un motif (scénario 24)', async () => {
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Refuser Clara Lecomte' }))
    fireEvent.change(screen.getByLabelText('Motif du refus'), { target: { value: 'Portfolio vide' } })
    fireEvent.click(screen.getByRole('button', { name: 'Confirmer le refus' }))

    expect(await screen.findByText('Aucun compte en attente.')).toBeInTheDocument()
    expect(rejectCreator).toHaveBeenCalledWith(3, 'Portfolio vide', 'jeton')
  })

  it("affiche l'erreur du backend et garde le compte dans la liste", async () => {
    vi.mocked(validateCreator).mockRejectedValue({ status: 409, message: "Le profil créateur 3 n'est pas en attente de validation" })
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Valider Clara Lecomte' }))

    expect(await screen.findByRole('alert')).toHaveTextContent("n'est pas en attente")
    expect(screen.getByText('Clara Lecomte')).toBeInTheDocument()
  })

  it("désactive les boutons pendant l'envoi : un double-clic n'envoie qu'une décision (revue de #121)", async () => {
    let terminer
    vi.mocked(validateCreator).mockReturnValue(new Promise((resolve) => { terminer = resolve }))
    renderPage()

    const valider = await screen.findByRole('button', { name: 'Valider Clara Lecomte' })
    fireEvent.click(valider)
    fireEvent.click(valider)

    expect(valider).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Refuser Clara Lecomte' })).toBeDisabled()
    expect(validateCreator).toHaveBeenCalledTimes(1)

    terminer({ ...CLARA, validationStatus: 'VALIDATED' })
    expect(await screen.findByText('Aucun compte en attente.')).toBeInTheDocument()
  })

  it("réactive les boutons si l'envoi échoue", async () => {
    vi.mocked(validateCreator).mockRejectedValue({ status: 500, message: 'Erreur serveur' })
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Valider Clara Lecomte' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Erreur serveur')
    expect(screen.getByRole('button', { name: 'Valider Clara Lecomte' })).toBeEnabled()
  })

  // ── Rechargement à chaque nouveau token (#129) ─────────────────────────────

  it("efface l'erreur quand le chargement suivant réussit", async () => {
    vi.mocked(listPendingCreators)
      .mockRejectedValueOnce({ status: 401, message: 'Session expirée' })
      .mockResolvedValueOnce([{ userId: 4, displayName: 'Nouvelle Créatrice', city: 'Liège', country: 'Belgique', bio: '' }])
    const { rerender } = renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('Session expirée')

    auth.token = 'jeton-renouvele'
    rerender(page())

    expect(await screen.findByText('Nouvelle Créatrice')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('ignore une réponse arrivée après une plus récente', async () => {
    let repondreAncien
    vi.mocked(listPendingCreators)
      .mockReturnValueOnce(new Promise((resolve) => { repondreAncien = resolve }))
      .mockResolvedValueOnce([{ userId: 4, displayName: 'Nouvelle Créatrice', city: 'Liège', country: 'Belgique', bio: '' }])
    const { rerender } = renderPage()

    auth.token = 'jeton-renouvele'
    rerender(page())
    expect(await screen.findByText('Nouvelle Créatrice')).toBeInTheDocument()

    repondreAncien([{ userId: 9, displayName: 'Ancienne réponse', city: null, country: null, bio: '' }])
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)))

    expect(screen.queryByText('Ancienne réponse')).not.toBeInTheDocument()
    expect(screen.getByText('Nouvelle Créatrice')).toBeInTheDocument()
  })
})
