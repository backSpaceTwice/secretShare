import { Routes, Route } from 'react-router-dom'
import CreateSecret from './pages/CreateSecret'
import ViewSecret from './pages/ViewSecret'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<CreateSecret />} />
      <Route path="/secrets/:token" element={<ViewSecret />} />
    </Routes>
  )
}
