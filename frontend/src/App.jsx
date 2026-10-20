import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Galerie from './pages/Galerie'
import MonProfilCreateur from './pages/MonProfilCreateur'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/galerie" element={<Galerie />} />
        <Route path="/mon-profil-createur" element={<MonProfilCreateur />} />
      </Routes>
    </BrowserRouter>
  )
}
