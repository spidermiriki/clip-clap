import { BrowserRouter, Route, Routes } from 'react-router-dom'
import MonProfilCreateur from './pages/MonProfilCreateur'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/mon-profil-createur" element={<MonProfilCreateur />} />
      </Routes>
    </BrowserRouter>
  )
}
