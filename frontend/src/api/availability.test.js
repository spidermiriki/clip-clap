import { afterEach, describe, expect, it, vi } from 'vitest'
import { getAvailableSlots } from './availability'

function reponse(status, body) {
  return { ok: status < 400, status, json: async () => body }
}

describe('getAvailableSlots (#28, utilisée par la réservation #29)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("interroge les créneaux publics d'un créateur pour une date et une prestation", async () => {
    const fetchMock = vi.fn(async () => reponse(200, [{ id: 1, start: '2027-04-02T14:00:00+02:00', end: '2027-04-02T15:00:00+02:00' }]))
    vi.stubGlobal('fetch', fetchMock)

    const slots = await getAvailableSlots(3, '2027-04-02', 5)

    expect(slots).toHaveLength(1)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/offerings/creators/3/slots?date=2027-04-02&offeringId=5')
    expect(options).toBeUndefined()
  })

  it('omet offeringId quand il est absent', async () => {
    const fetchMock = vi.fn(async () => reponse(200, []))
    vi.stubGlobal('fetch', fetchMock)

    await getAvailableSlots(3, '2027-04-02')

    expect(fetchMock.mock.calls[0][0]).toBe('/api/offerings/creators/3/slots?date=2027-04-02')
  })
})
