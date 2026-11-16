// URL complète d'un appel au backend (#106). VITE_API_BASE_URL est vide en dev : les appels
// restent relatifs et passent par le proxy Vite ; en production (frontend sur Vercel,
// backend sur Render), elle contient l'URL Render. Lue à chaque appel, pas à l'import.
export function apiUrl(path) {
  const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '')
  return `${base}${path}`
}
