import { Routes, Route } from 'react-router-dom'
import CreateSecret from './pages/CreateSecret'
import ViewSecret from './pages/ViewSecret'
import Layout from './components/Layout'

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<CreateSecret />} />
        <Route path="/secrets/:token" element={<ViewSecret />} />
      </Routes>
    </Layout>
  )
}
