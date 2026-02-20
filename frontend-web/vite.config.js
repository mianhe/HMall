import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5174,
    proxy: {
      '/api/cart': { target: 'http://127.0.0.1:8087', changeOrigin: true },
      '/api': { target: 'http://127.0.0.1:8085', changeOrigin: true },
    },
  },
})
