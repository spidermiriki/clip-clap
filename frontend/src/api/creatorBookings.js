// Module API — réservations reçues par le créateur connecté (contrat de #31, utilisé par #32)

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

/** { counts: { EN_ATTENTE: n, ... }, bookings: [BookingView, ...] } */
export async function getCreatorBookings(token) {
  const res = await fetch(apiUrl('/api/bookings/creator'), { headers: headers(token) })
  return handleResponse(res, 'Impossible de charger les réservations reçues.')
}

/** En attente → Confirmée ; renvoie la BookingView mise à jour. */
export async function acceptBooking(bookingId, token) {
  const res = await fetch(apiUrl(`/api/bookings/${encodeURIComponent(bookingId)}/accept`), {
    method: 'POST',
    headers: headers(token),
  })
  return handleResponse(res, "Impossible d'accepter cette demande.")
}

/** Confirmée → Terminée (contrat de #37), une fois le créneau commencé ; renvoie la BookingView mise à jour. */
export async function completeBooking(bookingId, token) {
  const res = await fetch(apiUrl(`/api/bookings/${encodeURIComponent(bookingId)}/complete`), {
    method: 'POST',
    headers: headers(token),
  })
  return handleResponse(res, 'Impossible de marquer cette prestation comme terminée.')
}

/** En attente → Refusée, avec un message facultatif (500 caractères au plus). */
export async function refuseBooking(bookingId, message, token) {
  const res = await fetch(apiUrl(`/api/bookings/${encodeURIComponent(bookingId)}/refuse`), {
    method: 'POST',
    headers: headers(token, true),
    body: JSON.stringify(message ? { message } : {}),
  })
  return handleResponse(res, 'Impossible de refuser cette demande.')
}
