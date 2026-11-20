import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createOffering, deleteOffering, listMyOfferings, updateOffering } from '../api/offerings'
import MesPrestations from './MesPrestations'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'jeton' } }) }))
vi.mock('../api/offerings', () => ({
  listMyOfferings: vi.fn(),
  createOffering: vi.fn(),
  updateOffering: vi.fn(),
  deleteOffering: vi.fn(),
}))

const MARIAGE = {
  id: 5,
  name: 'Reportage mariage',
  durationMinutes: 480,
  basePrice: 1200,
  options: [{ id: 9, name: 'Drone', extraPrice: 150 }],
}

function renderPage() {
  return render(<MemoryRouter><MesPrestations /></MemoryRouter>)
}

describe('MesPrestations (#27)', () => {
  beforeEach(() => {
    vi.mocked(listMyOfferings).mockReset().mockResolvedValue([])
    vi.mocked(createOffering).mockReset()
    vi.mocked(updateOffering).mockReset()
    vi.mocked(deleteOffering).mockReset().mockResolvedValue(null)
  })

  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('crée une prestation avec une option (scénarios 10 et 11)', async () => {
    vi.mocked(createOffering).mockResolvedValue(MARIAGE)
    renderPage()
    await screen.findByText('Aucune prestation pour le moment.')

    fireEvent.change(screen.getByLabelText('Nom'), { target: { value: 'Reportage mariage' } })
    fireEvent.change(screen.getByLabelText('Durée (minutes)'), { target: { value: '480' } })
    fireEvent.change(screen.getByLabelText('Prix de base (€)'), { target: { value: '1200' } })
    fireEvent.click(screen.getByRole('button', { name: '+ Ajouter une option' }))
    fireEvent.change(screen.getByLabelText('Option 1'), { target: { value: 'Drone' } })
    fireEvent.change(screen.getByLabelText('Supplément (€)'), { target: { value: '150,50' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByText('Reportage mariage')).toBeInTheDocument()
    expect(createOffering).toHaveBeenCalledWith(
      { name: 'Reportage mariage', durationMinutes: 480, basePrice: 1200, options: [{ name: 'Drone', extraPrice: 150.5 }] },
      'jeton',
    )
    expect(screen.getByLabelText('Nom')).toHaveValue('')
  })

  it('modifie une prestation existante', async () => {
    vi.mocked(listMyOfferings).mockResolvedValue([MARIAGE])
    vi.mocked(updateOffering).mockResolvedValue({ ...MARIAGE, name: 'Mariage demi-journée', durationMinutes: 240 })
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Modifier Reportage mariage' }))
    expect(screen.getByLabelText('Option 1')).toHaveValue('Drone')
    fireEvent.change(screen.getByLabelText('Nom'), { target: { value: 'Mariage demi-journée' } })
    fireEvent.change(screen.getByLabelText('Durée (minutes)'), { target: { value: '240' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByText('Mariage demi-journée')).toBeInTheDocument()
    expect(updateOffering).toHaveBeenCalledWith(5, expect.objectContaining({ durationMinutes: 240 }), 'jeton')
  })

  it('affiche le refus du backend quand la prestation est réservée (scénario 40)', async () => {
    vi.mocked(listMyOfferings).mockResolvedValue([MARIAGE])
    vi.mocked(deleteOffering).mockRejectedValue({ status: 409, message: 'Cette prestation est liée à une réservation.' })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Supprimer Reportage mariage' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Cette prestation est liée à une réservation.')
    expect(screen.getByText('Reportage mariage')).toBeInTheDocument()
  })

  it('supprime une prestation après confirmation', async () => {
    vi.mocked(listMyOfferings).mockResolvedValue([MARIAGE])
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Supprimer Reportage mariage' }))

    expect(await screen.findByText('Aucune prestation pour le moment.')).toBeInTheDocument()
    expect(deleteOffering).toHaveBeenCalledWith(5, 'jeton')
  })

  it('invite à créer son profil si le backend répond 404', async () => {
    vi.mocked(listMyOfferings).mockRejectedValue({ status: 404, message: 'Profil introuvable' })
    renderPage()

    expect(await screen.findByRole('link', { name: 'Créer mon profil' })).toBeInTheDocument()
  })
})
