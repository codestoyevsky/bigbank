/// <reference types="vitest/config" />
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [vue()],
  build: {
    // Roughly the last two years of Chrome, Edge, Firefox and Safari.
    target: ['chrome111', 'edge111', 'firefox114', 'safari16.4'],
  },
  server: {
    proxy: {
      '/api': { target: process.env.API_URL ?? 'http://localhost:8080', changeOrigin: true },
    },
  },
  test: {
    environment: 'jsdom',
    restoreMocks: true,
    setupFiles: ['src/__tests__/setup.ts'],
    include: ['src/**/*.test.ts'],
  },
})
