import { svelte } from '@sveltejs/vite-plugin-svelte';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  // The sveltekit() plugin from vite.config.ts is not used here: it owns route
  // resolution and expects a dev/build server, which breaks under Vitest. The
  // plain svelte plugin compiles components, and $app/* is aliased below.
  plugins: [svelte({ hot: false })],
  resolve: {
    conditions: ['browser'],
    alias: {
      $app: new URL('./src/testing/app', import.meta.url).pathname,
      $lib: new URL('./src/lib', import.meta.url).pathname,
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.spec.ts'],
    setupFiles: ['./src/testing/setup.ts'],
  },
});
