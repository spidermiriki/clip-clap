import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getCategories } from '../api/catalog'
import { createMyProfile, getMyCategories, getMyProfile, updateMyCategories, updateMyProfile } from '../api/creators'
import MonProfilCreateur from './MonProfilCreateur'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'jeton' } }) }))
vi.mock('../api/catalog', () => ({ getCategories: vi.fn() }))
vi.mock('../api/creators', () => ({
  getMyProfile: vi.fn(),
  createMyProfile: vi.fn(),
  updateMyProfile: vi.fn(),
  getMyCategories: vi.fn(),
  updateMyCategories: vi.fn(),
}))

const PROFIL = { userId: 1, displayName: 'Alice', bio: '', city: 'Mons', country: 'Belgique', serviceAreas: '' }
const CATEGORIES = [{ id: 1, name: 'Mariage' }, { id: 3, name: 'Sport' }, { id: 6, name: 'Portrait' }]

describe('MonProfilCreateur : types de prestation (#87)', () => {
  beforeEach(() => {
    vi.mocked(getCategories).mockReset().mockResolvedValue(CATEGORIES)
    vi.mocked(getMyProfile).mockReset().mockResolvedValue(PROFIL)
    vi.mocked(getMyCategories).mockReset().mockResolvedValue([1])
    vi.mocked(updateMyProfile).mockReset().mockResolvedValue(PROFIL)
    vi.mocked(createMyProfile).mockReset().mockResolvedValue(PROFIL)
    vi.mocked(updateMyCategories).mockReset().mockImplementation(async (ids) => [...ids].sort((a, b) => a - b))
  })

  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(cleanup)

  it('coche les types déjà choisis et enregistre le nouveau choix avec le profil', async () => {
    render(<MonProfilCreateur />)

    expect(await screen.findByRole('checkbox', { name: 'Mariage' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Sport' })).not.toBeChecked()

    fireEvent.click(screen.getByRole('checkbox', { name: 'Mariage' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Sport' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Portrait' }))
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByText('Profil enregistré.')).toBeInTheDocument()
    expect(updateMyProfile).toHaveBeenCalled()
    expect(updateMyCategories).toHaveBeenCalledWith([3, 6], 'jeton')
  })

  it('à la création, enregistre les types après le profil', async () => {
    vi.mocked(getMyProfile).mockResolvedValue(null)
    render(<MonProfilCreateur />)

    fireEvent.change(await screen.findByLabelText('Nom affiché *'), { target: { value: 'Alice' } })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Sport' }))
    fireEvent.click(screen.getByRole('button', { name: 'Créer le profil' }))

    expect(await screen.findByText('Profil enregistré.')).toBeInTheDocument()
    expect(getMyCategories).not.toHaveBeenCalled()
    expect(vi.mocked(createMyProfile).mock.invocationCallOrder[0])
      .toBeLessThan(vi.mocked(updateMyCategories).mock.invocationCallOrder[0])
    expect(updateMyCategories).toHaveBeenCalledWith([3], 'jeton')
  })

  it('garde le formulaire utilisable si les types ne peuvent pas être chargés', async () => {
    vi.mocked(getCategories).mockRejectedValue(new TypeError('Failed to fetch'))
    render(<MonProfilCreateur />)

    expect(await screen.findByText('Les types de prestation ne peuvent pas être chargés pour le moment.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByText('Profil enregistré.')).toBeInTheDocument()
    expect(updateMyCategories).not.toHaveBeenCalled()
  })

  it('remplit le formulaire même si les types choisis ne se chargent pas, sans les écraser (revue de #122)', async () => {
    vi.mocked(getMyCategories).mockRejectedValue({ status: 500, message: 'Erreur serveur (500).' })
    render(<MonProfilCreateur />)

    expect(await screen.findByDisplayValue('Alice')).toBeInTheDocument()
    expect(await screen.findByText('Les types de prestation ne peuvent pas être chargés pour le moment.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByText('Profil enregistré.')).toBeInTheDocument()
    expect(updateMyProfile).toHaveBeenCalledWith(expect.objectContaining({ displayName: 'Alice', city: 'Mons' }), 'jeton')
    expect(updateMyCategories).not.toHaveBeenCalled()
  })

  it("n'affiche pas de formulaire si le profil ne peut pas être chargé : jamais de PUT avec des champs vides", async () => {
    vi.mocked(getMyProfile).mockRejectedValue({ status: 500, message: 'Erreur serveur (500).' })
    render(<MonProfilCreateur />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de charger le profil.')
    expect(screen.queryByRole('button', { name: 'Enregistrer' })).not.toBeInTheDocument()
    expect(updateMyProfile).not.toHaveBeenCalled()
  })
})
