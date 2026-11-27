import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getCreatorProfile } from '../api/catalog'
import { getAvailableSlots } from '../api/availability'
import { createBooking } from '../api/bookings'
import Reservation from './Reservation'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'jeton' } }) }))
vi.mock('../api/catalog', () => ({ getCreatorProfile: vi.fn() }))
vi.mock('../api/availability', () => ({ getAvailableSlots: vi.fn() }))
vi.mock('../api/bookings', () => ({ createBooking: vi.fn() }))

const PROFIL = {
  userId: 3,
  displayName: 'Clara Lecomte',
  offerings: [
    {
      id: 5,
      name: 'Reportage mariage',
      durationMinutes: 480,
      basePrice: 1200,
      options: [{ id: 9, name: 'Drone', extraPrice: 150 }],
    },
  ],
}

const CRENEAU = { id: 7, start: '2027-04-02T14:00:00+02:00', end: '2027-04-02T22:00:00+02:00' }

function renderPage(url = '/createurs/3/reserver') {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Routes>
        <Route path="/createurs/:userId" element={<p>Profil</p>} />
        <Route path="/createurs/:userId/reserver" element={<Reservation />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('Reservation (#29, scénarios 14 et 22)', () => {
  beforeEach(() => {
    vi.mocked(getCreatorProfile).mockReset().mockResolvedValue(PROFIL)
    vi.mocked(getAvailableSlots).mockReset().mockResolvedValue([CRENEAU])
    vi.mocked(createBooking).mockReset()
  })

  afterEach(cleanup)

  async function choisirPrestationCreneauEtOption() {
    renderPage()
    fireEvent.change(await screen.findByRole('combobox'), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText('Date'), { target: { value: '2027-04-02' } })
    fireEvent.click(await screen.findByRole('button', { name: '14:00 – 22:00' }))
  }

  it('charge les créneaux avec le jeton (#28/#29 : authentification exigée)', async () => {
    await choisirPrestationCreneauEtOption()

    expect(getAvailableSlots).toHaveBeenCalledWith('3', '2027-04-02', '5', 'jeton')
  })

  it('calcule le total à partir du prix de base et des options cochées', async () => {
    await choisirPrestationCreneauEtOption()

    expect(screen.getByText(/Total : 1\s?200,00\s€/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('checkbox', { name: /Drone/ }))
    expect(screen.getByText(/Total : 1\s?350,00\s€/)).toBeInTheDocument()
  })

  it('envoie la réservation choisie et affiche le statut En attente (scénario 14)', async () => {
    vi.mocked(createBooking).mockResolvedValue({ id: 1, status: 'EN_ATTENTE', totalPrice: 1200 })
    await choisirPrestationCreneauEtOption()

    fireEvent.click(screen.getByRole('button', { name: 'Envoyer' }))

    expect(await screen.findByText('Statut : En attente')).toBeInTheDocument()
    expect(createBooking).toHaveBeenCalledWith({ offeringId: 5, slotId: 7, optionIds: [] }, 'jeton')
  })

  it('affiche le refus du backend quand le créateur réserve sa propre prestation (scénario 22)', async () => {
    vi.mocked(createBooking).mockRejectedValue({ status: 403, message: 'Vous ne pouvez pas réserver votre propre prestation.' })
    await choisirPrestationCreneauEtOption()

    fireEvent.click(screen.getByRole('button', { name: 'Envoyer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Vous ne pouvez pas réserver votre propre prestation.')
  })

  it("annonce un créateur introuvable (404)", async () => {
    vi.mocked(getCreatorProfile).mockRejectedValue({ status: 404, message: 'Aucun créateur public' })
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent("Ce créateur n'existe pas ou n'est plus visible.")
  })

  it('désactive « Envoyer » tant que la prestation ou le créneau ne sont pas choisis', async () => {
    renderPage()
    await screen.findByText('Reportage mariage', { exact: false })

    expect(screen.getByRole('button', { name: 'Envoyer' })).toBeDisabled()
  })
})
