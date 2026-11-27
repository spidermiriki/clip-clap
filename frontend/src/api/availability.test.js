import { afterEach, describe, expect, it, vi } from 'vitest'
import { getAvailableSlots } from './availability'

function reponse(status, body) {
  return { ok: status < 400, status, json: async () => body }
}

describe('getAvailableSlots (#28, utilisée par la réservation #29)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("interroge les créneaux d'un créateur pour une date et une prestation, avec le jeton", async () => {
    const fetchMock = vi.fn(async () => reponse(200, [{ id: 1, start: '2027-04-02T14:00:00+02:00', end: '2027-04-02T15:00:00+02:00' }]))
    vi.stubGlobal('fetch', fetchMock)

    const slots = await getAvailableSlots(3, '2027-04-02', 5, 'jeton')

    expect(slots).toHaveLength(1)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/offerings/creators/3/slots?date=2027-04-02&offeringId=5')
    expect(options.headers.Authorization).toBe('Bearer jeton')
  })

  it('omet offeringId quand il est absent', async () => {
    const fetchMock = vi.fn(async () => reponse(200, []))
    vi.stubGlobal('fetch', fetchMock)

    await getAvailableSlots(3, '2027-04-02', undefined, 'jeton')

    expect(fetchMock.mock.calls[0][0]).toBe('/api/offerings/creators/3/slots?date=2027-04-02')
  })

  it('renvoie 401 sans jeton, comme tout le reste de l\'API sauf le catalogue', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => reponse(401, { title: 'Non authentifié' })))

    await expect(getAvailableSlots(3, '2027-04-02', 5)).rejects.toMatchObject({ status: 401 })
  })
})
