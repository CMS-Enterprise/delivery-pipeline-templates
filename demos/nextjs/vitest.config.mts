import { fileURLToPath } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // fileURLToPath, not URL.pathname: pathname percent-encodes, so a workspace
      // path containing a space resolves to a directory that does not exist.
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.spec.tsx'],
    setupFiles: ['./src/testing/setup.ts'],
  },
});
