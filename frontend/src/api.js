// Client API centralisé : évite de dupliquer la construction du header Authorization
// et le parsing JSON à chaque appel fetch (#11).
async function request(path, { method = 'GET', token, body } = {}) {
  const response = await fetch(path, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.status === 204 ? null : response.json()
}

export function getJson(path, token) {
  return request(path, { token })
}

export function postJson(path, body, token) {
  return request(path, { method: 'POST', token, body })
}
