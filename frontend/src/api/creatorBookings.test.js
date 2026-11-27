import { afterEach, describe, expect, it, vi } from 'vitest'
import { acceptBooking, completeBooking, getCreatorBookings, refuseBooking } from './creatorBookings'

function reponse(status, body) {
  return { ok: status < 400, status, json: async () => body }
}

describe('creatorBookings (contrat de #31)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('charge les réservations reçues avec le jeton', async () => {
    const corps = { counts: { EN_ATTENTE: 1 }, bookings: [{ id: 12, status: 'EN_ATTENTE' }] }
    const fetchMock = vi.fn(async () => reponse(200, corps))
    vi.stubGlobal('fetch', fetchMock)

    expect(await getCreatorBookings('jeton')).toEqual(corps)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/bookings/creator')
    expect(options.method).toBeUndefined()
    expect(options.headers.Authorization).toBe('Bearer jeton')
  })

  it('accepte une demande par un POST sans corps', async () => {
    const fetchMock = vi.fn(async () => reponse(200, { id: 12, status: 'CONFIRMEE' }))
    vi.stubGlobal('fetch', fetchMock)

    expect(await acceptBooking(12, 'jeton')).toEqual({ id: 12, status: 'CONFIRMEE' })
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/bookings/12/accept')
    expect(options.method).toBe('POST')
    expect(options.headers.Authorization).toBe('Bearer jeton')
    expect(options.body).toBeUndefined()
  })

  it('refuse une demande en envoyant le message', async () => {
    const fetchMock = vi.fn(async () => reponse(200, { id: 12, status: 'REFUSEE', refusalMessage: 'Déjà pris ce jour-là' }))
    vi.stubGlobal('fetch', fetchMock)

    await refuseBooking(12, 'Déjà pris ce jour-là', 'jeton')

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/bookings/12/refuse')
    expect(options.method).toBe('POST')
    expect(options.headers.Authorization).toBe('Bearer jeton')
    expect(options.headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(options.body)).toEqual({ message: 'Déjà pris ce jour-là' })
  })

  it("refuse sans message en envoyant un corps vide", async () => {
    const fetchMock = vi.fn(async () => reponse(200, { id: 12, status: 'REFUSEE' }))
    vi.stubGlobal('fetch', fetchMock)

    await refuseBooking(12, '', 'jeton')

    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({})
  })

  it('remonte le détail du ProblemDetail (409 transition invalide)', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => reponse(409, { title: 'Transition invalide', detail: 'Transition interdite : CONFIRMEE → CONFIRMEE' })))

    await expect(acceptBooking(12, 'jeton'))
      .rejects.toEqual({ status: 409, message: 'Transition interdite : CONFIRMEE → CONFIRMEE' })
  })

  it('se rabat sur le titre, puis sur le message par défaut', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => reponse(403, { title: 'Accès refusé' })))
    await expect(refuseBooking(12, '', 'jeton')).rejects.toEqual({ status: 403, message: 'Accès refusé' })

    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false, status: 500, json: async () => { throw new Error('pas de JSON') } })))
    await expect(getCreatorBookings('jeton'))
      .rejects.toEqual({ status: 500, message: 'Impossible de charger les réservations reçues.' })
  })

  it('marque une prestation comme terminée par un POST sans corps (contrat de #37)', async () => {
    const fetchMock = vi.fn(async () => reponse(200, { id: 13, status: 'TERMINEE' }))
    vi.stubGlobal('fetch', fetchMock)

    expect(await completeBooking(13, 'jeton')).toEqual({ id: 13, status: 'TERMINEE' })
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/bookings/13/complete')
    expect(options.method).toBe('POST')
    expect(options.headers.Authorization).toBe('Bearer jeton')
    expect(options.body).toBeUndefined()
  })

  it('remonte les deux 409 du passage à « Terminée »', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => reponse(409, { title: 'Transition invalide', detail: 'Transition interdite : ANNULEE → TERMINEE' })))
    await expect(completeBooking(13, 'jeton'))
      .rejects.toEqual({ status: 409, message: 'Transition interdite : ANNULEE → TERMINEE' })

    vi.stubGlobal('fetch', vi.fn(async () => reponse(409, { title: 'Prestation pas encore commencée' })))
    await expect(completeBooking(13, 'jeton'))
      .rejects.toEqual({ status: 409, message: 'Prestation pas encore commencée' })
  })
})
