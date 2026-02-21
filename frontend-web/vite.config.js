import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5174,
    proxy: {
      '/api': { target: 'http://localhost:8085', changeOrigin: true },
    },
  },
})
