// Module API — réservation d'un créneau par un client (#29)

import { apiUrl } from './baseUrl'
import { readProblemMessage } from './problemDetail'

function headers(token, json = false) {
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(json ? { 'Content-Type': 'application/json' } : {}),
  }
}

async function handleResponse(res, fallback) {
  if (!res.ok) throw { status: res.status, message: await readProblemMessage(res, fallback) }
  return res.status === 204 ? null : res.json()
}

/** booking : { offeringId, slotId, optionIds: [id, ...] } */
export async function createBooking(booking, token) {
  const res = await fetch(apiUrl('/api/bookings'), {
    method: 'POST',
    headers: headers(token, true),
    body: JSON.stringify(booking),
  })
  return handleResponse(res, "Impossible d'envoyer la demande de réservation.")
}
