import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
  resolve: {
    alias: {
      '@/components': path.resolve(__dirname, './src/components'),
      '@/contexts':   path.resolve(__dirname, './src/contexts'),
      '@/types':      path.resolve(__dirname, './src/types'),
      '@/services':   path.resolve(__dirname, './src/services'),
      '@/views':      path.resolve(__dirname, './src/views'),
      '@/hooks':      path.resolve(__dirname, './src/hooks'),
      '@/utils':      path.resolve(__dirname, './src/utils'),
      '@/constants':  path.resolve(__dirname, './src/constants'),
    },
  },
})
