// Message lisible d'une réponse d'erreur ProblemDetail (RFC 9457) : detail, puis title,
// puis le message de repli si le corps est absent ou n'est pas du JSON.
export async function readProblemMessage(res, fallback) {
  const body = await res.json().catch(() => null)
  return body?.detail || body?.title || fallback
}
