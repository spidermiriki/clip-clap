import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthProvider } from 'react-oidc-context'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import Callback from './pages/Callback.jsx'
import Galerie from './pages/Galerie.jsx'
import Inscription from './pages/Inscription.jsx'
import MesAlbums from './pages/MesAlbums.jsx'
import MesPrestations from './pages/MesPrestations.jsx'
import MonProfilCreateur from './pages/MonProfilCreateur.jsx'
import ProfilPublic from './pages/ProfilPublic.jsx'
import { oidcConfig } from './auth/oidcConfig.js'
import ProtectedRoute from './auth/ProtectedRoute.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider {...oidcConfig}>
        <Routes>
          <Route element={<App />}>
            <Route index element={<Galerie />} />
            {/* Public, comme la galerie (#17) */}
            <Route path="createurs/:userId" element={<ProfilPublic />} />
            <Route
              path="mon-profil"
              element={(
                <ProtectedRoute roles={['CREATOR']}>
                  <MonProfilCreateur />
                </ProtectedRoute>
              )}
            />
            <Route
              path="mes-albums"
              element={(
                <ProtectedRoute roles={['CREATOR']}>
                  <MesAlbums />
                </ProtectedRoute>
              )}
            />
            <Route
              path="mes-prestations"
              element={(
                <ProtectedRoute roles={['CREATOR']}>
                  <MesPrestations />
                </ProtectedRoute>
              )}
            />
            <Route path="inscription" element={<Inscription />} />
          </Route>
          {/* Hors de la mise en page commune : pas de nav pendant le traitement du retour Zitadel */}
          <Route path="callback" element={<Callback />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
