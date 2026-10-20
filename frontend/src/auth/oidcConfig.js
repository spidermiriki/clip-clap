// Configuration OIDC (Zitadel, flux Authorization Code + PKCE).
// Ces valeurs ne sont pas secrètes ; elles peuvent être remplacées par les variables
// VITE_ZITADEL_* d'un fichier .env.local (voir frontend/.env.example).
const issuer = import.meta.env.VITE_ZITADEL_ISSUER ?? 'https://clic-clap-jiufbb.eu1.zitadel.cloud'
const clientId = import.meta.env.VITE_ZITADEL_CLIENT_ID ?? '395953354926226893'
const projectId = import.meta.env.VITE_ZITADEL_PROJECT_ID ?? '395951731327930510'

export const oidcConfig = {
  authority: issuer,
  client_id: clientId,
  redirect_uri: `${window.location.origin}/callback`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  // email : lu par le backend à la création du compte ;
  // audience du projet : exigée par le backend ; roles : utilisés à partir de #11
  scope: `openid profile email urn:zitadel:iam:org:project:id:${projectId}:aud urn:zitadel:iam:org:projects:roles`,
  // Après le retour de Zitadel, retire le code de l'URL et revient à l'accueil
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, '/')
  },
}
