// Configuration OIDC (Zitadel, flux Authorization Code + PKCE).
// Ces valeurs ne sont pas secrètes ; elles peuvent être remplacées par les variables
// VITE_ZITADEL_* d'un fichier .env.local (voir frontend/.env.example).
// `||` et non `??` : un build arg Docker non renseigné arrive en chaîne vide, pas en undefined
const issuer = import.meta.env.VITE_ZITADEL_ISSUER || 'https://clic-clap-jiufbb.eu1.zitadel.cloud'
const clientId = import.meta.env.VITE_ZITADEL_CLIENT_ID || '395953354926226893'
const projectId = import.meta.env.VITE_ZITADEL_PROJECT_ID || '395951731327930510'

export const oidcConfig = {
  authority: issuer,
  client_id: clientId,
  redirect_uri: `${window.location.origin}/callback`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  // email : lu par le backend à la création du compte ;
  // audience du projet : exigée par le backend ; roles : le claim de rôles Zitadel (#11) ;
  // offline_access : permet le renouvellement automatique du token via un refresh token,
  // sans iframe cachée ni dépendance aux cookies tiers (#16)
  scope: `openid profile email offline_access urn:zitadel:iam:org:project:id:${projectId}:aud urn:zitadel:iam:org:projects:roles`,
  // oidc-client-ts ne charge pas les claims étendues (email, rôles, profil) par défaut :
  // l'ID token de Zitadel ne contient que les claims minimales. loadUserInfo déclenche un
  // appel à l'endpoint userinfo de Zitadel après la connexion, dont le résultat est fusionné
  // dans `user.profile` - c'est ce qui rend le rôle (et l'e-mail) réellement disponibles (#16).
  loadUserInfo: true,
  // Pas d'onSigninCallback ici : la page /callback (src/pages/Callback.jsx) gère elle-même
  // la redirection finale avec react-router, une fois le rôle vérifié ou attribué.
  // automaticSilentRenew est à true par défaut dans oidc-client-ts : avec offline_access,
  // le renouvellement se fait via le refresh token, de façon transparente pour l'utilisateur.
}
