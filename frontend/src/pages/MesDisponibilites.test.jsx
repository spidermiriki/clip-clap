import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createSlot, deleteSlot, getMySettings, listMySlots, saveMySettings } from '../api/availability'
import MesDisponibilites from './MesDisponibilites'

// Token modifiable : simule le renouvellement silencieux de la session (#129)
const auth = vi.hoisted(() => ({ token: 'jeton' }))
vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: auth.token } }) }))
vi.mock('../api/availability', () => ({
  listMySlots: vi.fn(),
  createSlot: vi.fn(),
  deleteSlot: vi.fn(),
  getMySettings: vi.fn(),
  saveMySettings: vi.fn(),
}))

const VENDREDI = { id: 4, creatorUserId: 1, start: '2027-04-02T14:00:00+02:00', end: '2027-04-02T16:00:00+02:00' }

const page = () => <MemoryRouter><MesDisponibilites /></MemoryRouter>

function renderPage() {
  return render(page())
}

describe('MesDisponibilites (#28)', () => {
  beforeEach(() => {
    auth.token = 'jeton'
    vi.mocked(listMySlots).mockReset().mockResolvedValue([])
    vi.mocked(getMySettings).mockReset().mockResolvedValue({ minNoticeHours: 0, bufferMinutes: 0 })
    vi.mocked(createSlot).mockReset()
    vi.mocked(deleteSlot).mockReset().mockResolvedValue(null)
    vi.mocked(saveMySettings).mockReset()
  })

  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('ajoute un créneau à l\'heure de Bruxelles et le range sous son jour (scénario 12)', async () => {
    vi.mocked(createSlot).mockResolvedValue(VENDREDI)
    renderPage()
    await screen.findByText('Aucun créneau pour les 30 prochains jours.')

    fireEvent.change(screen.getByLabelText('Date'), { target: { value: '2027-04-02' } })
    fireEvent.change(screen.getByLabelText('Début'), { target: { value: '14:00' } })
    fireEvent.change(screen.getByLabelText('Fin'), { target: { value: '16:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ajouter' }))

    const liste = await screen.findByRole('list', { name: /Créneaux du vendredi 2 avril 2027/ })
    expect(within(liste).getByText('14:00 – 16:00')).toBeInTheDocument()
    expect(createSlot).toHaveBeenCalledWith('2027-04-02T14:00', '2027-04-02T16:00', 'jeton')
  })

  it('affiche les heures de Bruxelles telles quelles, quel que soit le fuseau du navigateur', async () => {
    vi.mocked(listMySlots).mockResolvedValue([{ ...VENDREDI, start: '2027-03-28T03:30:00+02:00', end: '2027-03-28T04:30:00+02:00' }])
    renderPage()

    expect(await screen.findByText('03:30 – 04:30')).toBeInTheDocument()
  })

  it('affiche le refus du backend pour un créneau qui chevauche (409)', async () => {
    vi.mocked(createSlot).mockRejectedValue({ status: 409, message: 'Ce créneau chevauche un de vos créneaux existants' })
    renderPage()
    await screen.findByText('Aucun créneau pour les 30 prochains jours.')

    fireEvent.change(screen.getByLabelText('Date'), { target: { value: '2027-04-02' } })
    fireEvent.change(screen.getByLabelText('Début'), { target: { value: '15:00' } })
    fireEvent.change(screen.getByLabelText('Fin'), { target: { value: '17:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ajouter' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('chevauche')
  })

  it('supprime un créneau après confirmation', async () => {
    vi.mocked(listMySlots).mockResolvedValue([VENDREDI])
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: /Supprimer le créneau de 14:00/ }))

    expect(await screen.findByText('Aucun créneau pour les 30 prochains jours.')).toBeInTheDocument()
    expect(deleteSlot).toHaveBeenCalledWith(4, 'jeton')
  })

  it('enregistre le préavis et la marge (scénario 13)', async () => {
    vi.mocked(saveMySettings).mockResolvedValue({ minNoticeHours: 48, bufferMinutes: 30 })
    renderPage()

    fireEvent.change(await screen.findByLabelText('Préavis minimum (heures)'), { target: { value: '48' } })
    fireEvent.change(screen.getByLabelText('Marge entre deux rendez-vous (minutes)'), { target: { value: '30' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer les réglages' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Réglages enregistrés.')
    expect(saveMySettings).toHaveBeenCalledWith({ minNoticeHours: 48, bufferMinutes: 30 }, 'jeton')
  })

  it('invite à créer son profil si le backend répond 404', async () => {
    vi.mocked(listMySlots).mockRejectedValue({ status: 404, message: 'Profil introuvable' })
    renderPage()

    expect(await screen.findByRole('link', { name: 'Créer mon profil' })).toBeInTheDocument()
  })

  // ── Rechargement à chaque nouveau token (#129) ─────────────────────────────

  it("efface l'erreur quand le chargement suivant réussit", async () => {
    vi.mocked(listMySlots)
      .mockRejectedValueOnce({ status: 401, message: 'Session expirée' })
      .mockResolvedValueOnce([{ id: 5, creatorUserId: 1, start: '2027-04-02T10:00:00+02:00', end: '2027-04-02T11:00:00+02:00' }])
    const { rerender } = renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('Session expirée')

    auth.token = 'jeton-renouvele'
    rerender(page())

    expect(await screen.findByText('10:00 – 11:00')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it("quitte l'écran « pas de profil » quand le chargement suivant réussit", async () => {
    vi.mocked(listMySlots)
      .mockRejectedValueOnce({ status: 404, message: 'Profil introuvable' })
      .mockResolvedValueOnce([{ id: 5, creatorUserId: 1, start: '2027-04-02T10:00:00+02:00', end: '2027-04-02T11:00:00+02:00' }])
    const { rerender } = renderPage()
    await screen.findByRole('link', { name: 'Créer mon profil' })

    auth.token = 'jeton-renouvele'
    rerender(page())

    expect(await screen.findByText('10:00 – 11:00')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Créer mon profil' })).not.toBeInTheDocument()
  })

  it('ignore une réponse arrivée après une plus récente', async () => {
    let repondreAncien
    vi.mocked(listMySlots)
      .mockReturnValueOnce(new Promise((resolve) => { repondreAncien = resolve }))
      .mockResolvedValueOnce([{ id: 5, creatorUserId: 1, start: '2027-04-02T10:00:00+02:00', end: '2027-04-02T11:00:00+02:00' }])
    const { rerender } = renderPage()

    auth.token = 'jeton-renouvele'
    rerender(page())
    expect(await screen.findByText('10:00 – 11:00')).toBeInTheDocument()

    repondreAncien([{ id: 9, creatorUserId: 1, start: '2027-04-02T18:00:00+02:00', end: '2027-04-02T19:00:00+02:00' }])
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)))

    expect(screen.queryByText('18:00 – 19:00')).not.toBeInTheDocument()
    expect(screen.getByText('10:00 – 11:00')).toBeInTheDocument()
  })
})
