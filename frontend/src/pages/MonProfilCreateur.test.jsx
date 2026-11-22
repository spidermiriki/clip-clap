import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getMyProfile } from '../api/creators'
import MonProfilCreateur from './MonProfilCreateur'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'jeton' } }) }))
// « Mon profil » charge aussi les types de prestation (#87) : listes vides ici
vi.mock('../api/catalog', () => ({ getCategories: vi.fn(async () => []) }))
vi.mock('../api/creators', () => ({
  getMyProfile: vi.fn(),
  createMyProfile: vi.fn(),
  updateMyProfile: vi.fn(),
  getMyCategories: vi.fn(async () => []),
  updateMyCategories: vi.fn(async () => []),
}))

const PROFIL = { userId: 1, displayName: 'Alice', bio: '', city: 'Mons', country: 'Belgique', serviceAreas: '' }

describe('MonProfilCreateur : statut de validation (#33)', () => {
  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(cleanup)

  it('annonce un profil en attente de validation', async () => {
    vi.mocked(getMyProfile).mockResolvedValue({ ...PROFIL, validationStatus: 'PENDING', rejectionReason: null })
    render(<MonProfilCreateur />)

    expect(await screen.findByRole('status')).toHaveTextContent('en attente de validation')
  })

  it('affiche le motif du refus', async () => {
    vi.mocked(getMyProfile).mockResolvedValue({ ...PROFIL, validationStatus: 'REJECTED', rejectionReason: 'Portfolio vide' })
    render(<MonProfilCreateur />)

    expect(await screen.findByRole('status')).toHaveTextContent('Votre profil a été refusé : Portfolio vide.')
  })

  it("n'affiche rien pour un profil validé", async () => {
    vi.mocked(getMyProfile).mockResolvedValue({ ...PROFIL, validationStatus: 'VALIDATED', rejectionReason: null })
    render(<MonProfilCreateur />)

    expect(await screen.findByDisplayValue('Alice')).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
