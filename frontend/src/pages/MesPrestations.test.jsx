import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createOffering, deleteOffering, listMyOfferings, updateOffering } from '../api/offerings'
import MesPrestations from './MesPrestations'

// Token modifiable : simule le renouvellement silencieux de la session (revue de #119)
const auth = vi.hoisted(() => ({ token: 'jeton' }))
vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: auth.token } }) }))
vi.mock('../api/catalog', () => ({ getCategories: vi.fn(async () => [{ id: 1, name: 'Mariage' }, { id: 6, name: 'Portrait' }]) }))
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

const page = () => <MemoryRouter><MesPrestations /></MemoryRouter>

function renderPage() {
  return render(page())
}

describe('MesPrestations (#27)', () => {
  beforeEach(() => {
    auth.token = 'jeton'
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
      { name: 'Reportage mariage', durationMinutes: 480, basePrice: 1200, categoryId: null, options: [{ name: 'Drone', extraPrice: 150.5 }] },
      'jeton',
    )
    expect(screen.getByLabelText('Nom')).toHaveValue('')
  })

  it("enregistre la catégorie choisie, avec un prix « 1.200,00 », et l'affiche dans la liste (#123)", async () => {
    vi.mocked(createOffering).mockResolvedValue({ ...MARIAGE, categoryId: 1 })
    renderPage()
    await screen.findByRole('option', { name: 'Mariage' })

    fireEvent.change(screen.getByLabelText('Nom'), { target: { value: 'Reportage mariage' } })
    fireEvent.change(screen.getByLabelText('Durée (minutes)'), { target: { value: '480' } })
    fireEvent.change(screen.getByLabelText('Prix de base (€)'), { target: { value: '1.200,00' } })
    fireEvent.change(screen.getByLabelText('Catégorie'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByText(/^Mariage · 8 h · 1\s?200,00\s€$/)).toBeInTheDocument()
    expect(createOffering).toHaveBeenCalledWith(expect.objectContaining({ categoryId: 1, basePrice: 1200 }), 'jeton')
  })

  it('accepte un prix avec séparateur de milliers (revue de #119)', async () => {
    vi.mocked(createOffering).mockResolvedValue(MARIAGE)
    renderPage()
    await screen.findByText('Aucune prestation pour le moment.')

    fireEvent.change(screen.getByLabelText('Nom'), { target: { value: 'Reportage mariage' } })
    fireEvent.change(screen.getByLabelText('Durée (minutes)'), { target: { value: '480' } })
    fireEvent.change(screen.getByLabelText('Prix de base (€)'), { target: { value: '1.200,00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByText('Reportage mariage')).toBeInTheDocument()
    expect(createOffering).toHaveBeenCalledWith(expect.objectContaining({ basePrice: 1200 }), 'jeton')
  })

  it("n'envoie pas un prix illisible et explique le format attendu", async () => {
    renderPage()
    await screen.findByText('Aucune prestation pour le moment.')

    fireEvent.change(screen.getByLabelText('Nom'), { target: { value: 'Portrait' } })
    fireEvent.change(screen.getByLabelText('Durée (minutes)'), { target: { value: '60' } })
    fireEvent.change(screen.getByLabelText('Prix de base (€)'), { target: { value: '12,5,3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Format de prix invalide')
    expect(createOffering).not.toHaveBeenCalled()
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

  // ── Rechargement à chaque nouveau token (revue de #119) ───────────────────

  it("efface l'erreur quand le chargement suivant réussit", async () => {
    vi.mocked(listMyOfferings)
      .mockRejectedValueOnce({ status: 401, message: 'Session expirée' })
      .mockResolvedValueOnce([MARIAGE])
    const { rerender } = renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('Session expirée')

    auth.token = 'jeton-renouvele'
    rerender(page())

    expect(await screen.findByText('Reportage mariage')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it("quitte l'écran « pas de profil » quand le chargement suivant réussit", async () => {
    vi.mocked(listMyOfferings)
      .mockRejectedValueOnce({ status: 404, message: 'Profil introuvable' })
      .mockResolvedValueOnce([MARIAGE])
    const { rerender } = renderPage()
    await screen.findByRole('link', { name: 'Créer mon profil' })

    auth.token = 'jeton-renouvele'
    rerender(page())

    expect(await screen.findByText('Reportage mariage')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Créer mon profil' })).not.toBeInTheDocument()
  })

  it('ignore une réponse arrivée après une plus récente', async () => {
    let repondreAncien
    vi.mocked(listMyOfferings)
      .mockReturnValueOnce(new Promise((resolve) => { repondreAncien = resolve }))
      .mockResolvedValueOnce([MARIAGE])
    const { rerender } = renderPage()

    auth.token = 'jeton-renouvele'
    rerender(page())
    expect(await screen.findByText('Reportage mariage')).toBeInTheDocument()

    repondreAncien([{ ...MARIAGE, id: 99, name: 'Ancienne réponse' }])
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)))

    expect(screen.queryByText('Ancienne réponse')).not.toBeInTheDocument()
    expect(screen.getByText('Reportage mariage')).toBeInTheDocument()
  })
})
