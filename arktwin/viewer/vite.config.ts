import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  base: './',
  build: {
    outDir: 'dist/viewer',
  },
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:2237/',
        changeOrigin: true,
      },
    },
  },
})
