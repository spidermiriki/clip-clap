import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { acceptBooking, completeBooking, getCreatorBookings, refuseBooking } from '../api/creatorBookings'
import { getMyProfile } from '../api/creators'
import TableauDeBord from './TableauDeBord'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'jeton' } }) }))
vi.mock('../api/creatorBookings', () => ({
  getCreatorBookings: vi.fn(),
  acceptBooking: vi.fn(),
  refuseBooking: vi.fn(),
  completeBooking: vi.fn(),
}))
vi.mock('../api/creators', () => ({ getMyProfile: vi.fn() }))

const EN_ATTENTE = {
  id: 12,
  status: 'EN_ATTENTE',
  totalPrice: 1350,
  offering: { id: 5, name: 'Reportage mariage' },
  slot: { start: '2027-04-02T14:00:00+02:00', end: '2027-04-02T22:00:00+02:00' },
  client: { id: 8, email: 'client@exemple.be' },
  refusalMessage: null,
}

const CONFIRMEE = {
  id: 13,
  status: 'CONFIRMEE',
  totalPrice: 300,
  offering: null,
  slot: null,
  client: { id: 9, email: 'autre@exemple.be' },
  refusalMessage: null,
}

const COUNTS = { EN_ATTENTE: 1, CONFIRMEE: 1, REFUSEE: 1, ANNULEE: 2, EXPIREE: 0, TERMINEE: 1 }

function reponse(bookings = [EN_ATTENTE, CONFIRMEE], counts = COUNTS) {
  return { counts, bookings }
}

function ligne(texte) {
  return screen.getByText(texte).closest('li')
}

describe('TableauDeBord (#32, wireframe 08)', () => {
  beforeEach(() => {
    vi.mocked(getCreatorBookings).mockReset().mockResolvedValue(reponse())
    vi.mocked(acceptBooking).mockReset()
    vi.mocked(refuseBooking).mockReset()
    vi.mocked(completeBooking).mockReset()
    vi.mocked(getMyProfile).mockReset().mockResolvedValue({ validationStatus: 'VALIDATED' })
  })

  afterEach(cleanup)

  it('affiche les compteurs et le taux d’acceptation, sans compter les annulations', async () => {
    render(<TableauDeBord />)

    const indicateurs = await screen.findByRole('list', { name: 'Indicateurs' })
    expect(within(indicateurs).getByText('En attente').nextSibling).toHaveTextContent('1')
    expect(within(indicateurs).getByText('Confirmées').nextSibling).toHaveTextContent('1')
    // Confirmée + Terminée = 2, sur Confirmée + Terminée + Refusée + Expirée = 3
    expect(within(indicateurs).getByText("Taux d'acceptation").nextSibling)
      .toHaveTextContent(/^67\s?%$/)
    expect(within(indicateurs).getByText('Note').nextSibling).toHaveTextContent('—')
  })

  it('affiche « — » comme taux tant qu’aucune demande n’a été traitée', async () => {
    vi.mocked(getCreatorBookings).mockResolvedValue(reponse([], { EN_ATTENTE: 0, CONFIRMEE: 0, REFUSEE: 0, ANNULEE: 0, EXPIREE: 0, TERMINEE: 0 }))
    render(<TableauDeBord />)

    const indicateurs = await screen.findByRole('list', { name: 'Indicateurs' })
    expect(within(indicateurs).getByText("Taux d'acceptation").nextSibling).toHaveTextContent('—')
    expect(screen.getByText("Vous n'avez encore reçu aucune demande de réservation.")).toBeInTheDocument()
  })

  it('liste les réservations avec client, créneau, total et statut', async () => {
    render(<TableauDeBord />)

    const demande = (await screen.findByText('Reportage mariage')).closest('li')
    expect(demande).toHaveTextContent('vendredi 2 avril 2027, 14:00 – 22:00')
    expect(demande).toHaveTextContent('Client : client@exemple.be')
    expect(demande).toHaveTextContent(/Total : 1\s?350,00\s€/)
    expect(demande).toHaveTextContent('En attente')
    // Prestation et créneau supprimés depuis (ON DELETE SET NULL, #133)
    const confirmee = ligne('Prestation supprimée')
    expect(confirmee).toHaveTextContent('Créneau supprimé')
    expect(confirmee).toHaveTextContent('Confirmée')
  })

  it('ne propose « Accepter » et « Refuser » que sur les demandes en attente', async () => {
    render(<TableauDeBord />)

    const demande = (await screen.findByText('Reportage mariage')).closest('li')
    expect(within(demande).getByRole('button', { name: 'Accepter' })).toBeInTheDocument()
    expect(within(demande).getByRole('button', { name: 'Refuser' })).toBeInTheDocument()
    const confirmee = ligne('Prestation supprimée')
    expect(within(confirmee).queryByRole('button')).not.toBeInTheDocument()
  })

  it('accepte une demande puis recharge la liste', async () => {
    vi.mocked(acceptBooking).mockResolvedValue({ ...EN_ATTENTE, status: 'CONFIRMEE' })
    vi.mocked(getCreatorBookings)
      .mockResolvedValueOnce(reponse())
      .mockResolvedValueOnce(reponse([{ ...EN_ATTENTE, status: 'CONFIRMEE' }, CONFIRMEE], { ...COUNTS, EN_ATTENTE: 0, CONFIRMEE: 2 }))
    render(<TableauDeBord />)

    fireEvent.click(await screen.findByRole('button', { name: 'Accepter' }))

    expect(acceptBooking).toHaveBeenCalledWith(12, 'jeton')
    await vi.waitFor(() => expect(getCreatorBookings).toHaveBeenCalledTimes(2))
    await vi.waitFor(() => expect(screen.queryByRole('button', { name: 'Accepter' })).not.toBeInTheDocument())
    expect(ligne('Reportage mariage')).toHaveTextContent('Confirmée')
  })

  it('refuse une demande avec un message facultatif', async () => {
    vi.mocked(refuseBooking).mockResolvedValue({ ...EN_ATTENTE, status: 'REFUSEE' })
    render(<TableauDeBord />)

    fireEvent.click(await screen.findByRole('button', { name: 'Refuser' }))
    const champ = screen.getByLabelText('Message au client (facultatif)')
    expect(champ).toHaveAttribute('maxLength', '500')
    fireEvent.change(champ, { target: { value: '  Déjà pris ce jour-là  ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Confirmer le refus' }))

    await vi.waitFor(() => expect(refuseBooking).toHaveBeenCalledWith(12, 'Déjà pris ce jour-là', 'jeton'))
    await vi.waitFor(() => expect(getCreatorBookings).toHaveBeenCalledTimes(2))
  })

  it('referme le formulaire de refus sans rien envoyer', async () => {
    render(<TableauDeBord />)

    fireEvent.click(await screen.findByRole('button', { name: 'Refuser' }))
    fireEvent.click(screen.getByRole('button', { name: 'Annuler' }))

    expect(screen.queryByLabelText('Message au client (facultatif)')).not.toBeInTheDocument()
    expect(refuseBooking).not.toHaveBeenCalled()
  })

  it('affiche l’erreur du backend (409) et recharge la liste', async () => {
    vi.mocked(acceptBooking).mockRejectedValue({ status: 409, message: 'Transition interdite : ANNULEE → CONFIRMEE' })
    render(<TableauDeBord />)

    fireEvent.click(await screen.findByRole('button', { name: 'Accepter' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Transition interdite : ANNULEE → CONFIRMEE')
    await vi.waitFor(() => expect(getCreatorBookings).toHaveBeenCalledTimes(2))
  })

  it('affiche une erreur si les réservations ne peuvent pas être chargées', async () => {
    vi.mocked(getCreatorBookings).mockRejectedValue({ status: 500, message: 'Impossible de charger les réservations reçues.' })
    render(<TableauDeBord />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de charger les réservations reçues.')
  })

  it('affiche le bandeau si le profil attend sa validation', async () => {
    vi.mocked(getMyProfile).mockResolvedValue({ validationStatus: 'PENDING' })
    render(<TableauDeBord />)

    expect(await screen.findByRole('status')).toHaveTextContent('en attente de validation')
  })

  it('affiche le bandeau si le profil a été refusé', async () => {
    vi.mocked(getMyProfile).mockResolvedValue({ validationStatus: 'REJECTED', rejectionReason: 'Portfolio vide' })
    render(<TableauDeBord />)

    expect(await screen.findByRole('status')).toHaveTextContent('Votre profil a été refusé : Portfolio vide.')
  })

  it('n’affiche pas de bandeau pour un profil validé', async () => {
    render(<TableauDeBord />)

    await screen.findByText('Reportage mariage')
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})

describe('TableauDeBord : « Marquer comme terminée » (#38, API de #37)', () => {
  // Seule l'horloge est figée (Date), pas les minuteries : les findBy* continuent d'attendre normalement
  const MAINTENANT = new Date('2027-04-02T16:00:00+02:00')

  const COMMENCEE = {
    ...EN_ATTENTE,
    id: 20,
    status: 'CONFIRMEE',
    offering: { id: 5, name: 'Clip commencé' },
    slot: { start: '2027-04-02T14:00:00+02:00', end: '2027-04-02T18:00:00+02:00' },
  }
  const A_VENIR = {
    ...COMMENCEE,
    id: 21,
    offering: { id: 6, name: 'Clip à venir' },
    slot: { start: '2027-04-03T14:00:00+02:00', end: '2027-04-03T18:00:00+02:00' },
  }
  const TERMINEE = { ...COMMENCEE, id: 22, status: 'TERMINEE', offering: { id: 7, name: 'Clip terminé' } }
  const SANS_CRENEAU = { ...COMMENCEE, id: 23, slot: null, offering: { id: 8, name: 'Clip sans créneau' } }
  const EN_ATTENTE_PASSEE = { ...COMMENCEE, id: 24, status: 'EN_ATTENTE', offering: { id: 9, name: 'Demande passée' } }

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(MAINTENANT)
    vi.mocked(getCreatorBookings).mockReset()
      .mockResolvedValue(reponse([COMMENCEE, A_VENIR, TERMINEE, SANS_CRENEAU, EN_ATTENTE_PASSEE]))
    vi.mocked(completeBooking).mockReset()
    vi.mocked(getMyProfile).mockReset().mockResolvedValue({ validationStatus: 'VALIDATED' })
  })

  afterEach(() => {
    cleanup()
    vi.useRealTimers()
  })

  it('apparaît seulement sur une réservation confirmée dont le créneau a commencé', async () => {
    render(<TableauDeBord />)

    const commencee = (await screen.findByText('Clip commencé')).closest('li')
    expect(within(commencee).getByRole('button', { name: 'Marquer comme terminée' })).toBeInTheDocument()
    for (const nom of ['Clip à venir', 'Clip terminé', 'Clip sans créneau', 'Demande passée']) {
      expect(within(ligne(nom)).queryByRole('button', { name: 'Marquer comme terminée' })).not.toBeInTheDocument()
    }
  })

  it('marque la prestation comme terminée puis recharge la liste', async () => {
    vi.mocked(completeBooking).mockResolvedValue({ ...COMMENCEE, status: 'TERMINEE' })
    vi.mocked(getCreatorBookings)
      .mockResolvedValueOnce(reponse([COMMENCEE]))
      .mockResolvedValueOnce(reponse([{ ...COMMENCEE, status: 'TERMINEE' }]))
    render(<TableauDeBord />)

    fireEvent.click(await screen.findByRole('button', { name: 'Marquer comme terminée' }))

    expect(completeBooking).toHaveBeenCalledWith(20, 'jeton')
    await vi.waitFor(() => expect(screen.queryByRole('button', { name: 'Marquer comme terminée' })).not.toBeInTheDocument())
    expect(ligne('Clip commencé')).toHaveTextContent('Terminée')
  })

  it.each([
    ['Transition interdite : ANNULEE → TERMINEE'],
    ['Prestation pas encore commencée'],
  ])('affiche le 409 du backend (« %s ») et recharge la liste', async (message) => {
    vi.mocked(completeBooking).mockRejectedValue({ status: 409, message })
    render(<TableauDeBord />)

    fireEvent.click(await screen.findByRole('button', { name: 'Marquer comme terminée' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    await vi.waitFor(() => expect(getCreatorBookings).toHaveBeenCalledTimes(2))
  })
})
