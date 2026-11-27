import { afterEach, describe, expect, it, vi } from 'vitest'
import { createBooking } from './bookings'

function reponse(status, body) {
  return { ok: status < 400, status, json: async () => body }
}

describe('createBooking (#29)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('envoie la prestation, le créneau et les options, avec le jeton', async () => {
    const fetchMock = vi.fn(async () => reponse(201, { id: 1, status: 'EN_ATTENTE', totalPrice: 120 }))
    vi.stubGlobal('fetch', fetchMock)

    const booking = await createBooking({ offeringId: 5, slotId: 7, optionIds: [9] }, 'jeton')

    expect(booking).toEqual({ id: 1, status: 'EN_ATTENTE', totalPrice: 120 })
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/bookings')
    expect(options.method).toBe('POST')
    expect(options.headers.Authorization).toBe('Bearer jeton')
    expect(JSON.parse(options.body)).toEqual({ offeringId: 5, slotId: 7, optionIds: [9] })
  })

  it('remonte le détail du backend sur un créneau non réservable (409)', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => reponse(409, { title: 'Créneau non réservable', detail: "Ce créneau n'est plus réservable" })))

    await expect(createBooking({ offeringId: 5, slotId: 7, optionIds: [] }, 'jeton'))
      .rejects.toMatchObject({ status: 409, message: "Ce créneau n'est plus réservable" })
  })

  it("auto-réservation refusée (403)", async () => {
    vi.stubGlobal('fetch', vi.fn(async () => reponse(403, { title: 'Action refusée', detail: 'Vous ne pouvez pas réserver votre propre prestation.' })))

    await expect(createBooking({ offeringId: 5, slotId: 7, optionIds: [] }, 'jeton'))
      .rejects.toMatchObject({ status: 403 })
  })
})
