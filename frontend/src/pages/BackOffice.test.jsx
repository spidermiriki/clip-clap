import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { listPendingCreators, rejectCreator, validateCreator } from '../api/admin'
import BackOffice from './BackOffice'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'jeton' } }) }))
vi.mock('../api/admin', () => ({
  listPendingCreators: vi.fn(),
  validateCreator: vi.fn(),
  rejectCreator: vi.fn(),
}))

const CLARA = { userId: 3, displayName: 'Clara Lecomte', city: 'Mons', country: 'Belgique', bio: 'Mariages.' }

function renderPage() {
  return render(<MemoryRouter><BackOffice /></MemoryRouter>)
}

describe('BackOffice (#33)', () => {
  beforeEach(() => {
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
})
