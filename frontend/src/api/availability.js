// Module API — disponibilités du créateur connecté : créneaux libres, préavis et marge (#28).
// Les heures envoyées sont des heures locales de Bruxelles (« 2027-04-02T14:00 ») ; le backend
// renvoie des dates avec leur décalage (« 2027-04-02T14:00:00+02:00 »).

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

/** from et to : dates « AAAA-MM-JJ » (facultatives : les 30 prochains jours par défaut). */
export async function listMySlots(token, { from, to } = {}) {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const query = params.toString() ? `?${params}` : ''
  const res = await fetch(apiUrl(`/api/offerings/me/slots${query}`), { headers: headers(token) })
  return handleResponse(res, 'Impossible de charger vos créneaux.')
}

export async function createSlot(start, end, token) {
  const res = await fetch(apiUrl('/api/offerings/me/slots'), {
    method: 'POST',
    headers: headers(token, true),
    body: JSON.stringify({ start, end }),
  })
  return handleResponse(res, "Impossible d'ajouter le créneau.")
}

export async function deleteSlot(slotId, token) {
  const res = await fetch(apiUrl(`/api/offerings/me/slots/${slotId}`), { method: 'DELETE', headers: headers(token) })
  return handleResponse(res, 'Impossible de supprimer le créneau.')
}

export async function getMySettings(token) {
  const res = await fetch(apiUrl('/api/offerings/me/settings'), { headers: headers(token) })
  return handleResponse(res, 'Impossible de charger vos réglages.')
}

export async function saveMySettings(settings, token) {
  const res = await fetch(apiUrl('/api/offerings/me/settings'), {
    method: 'PUT',
    headers: headers(token, true),
    body: JSON.stringify(settings),
  })
  return handleResponse(res, "Impossible d'enregistrer vos réglages.")
}
