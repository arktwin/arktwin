import { createRoot } from 'react-dom/client'
import { App } from './functions.tsx'

const app = document.getElementById('app')
if (app != null) {
  createRoot(app).render(<App />)
}
