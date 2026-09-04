import { fileURLToPath } from 'node:url';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { defineConfig } from 'vitest/config';

// fileURLToPath, not URL.pathname: pathname percent-encodes, so a workspace path
// containing a space resolves to a directory that does not exist. Jenkins job
// names become workspace paths, and "Devops Pipeline" has one.
const dir = (path: string) => fileURLToPath(new URL(path, import.meta.url));

export default defineConfig({
  // The sveltekit() plugin from vite.config.ts is not used here: it owns route
  // resolution and expects a dev/build server, which breaks under Vitest. The
  // plain svelte plugin compiles components, and $app/* is aliased below.
  plugins: [svelte({ hot: false })],
  resolve: {
    conditions: ['browser'],
    alias: {
      $app: dir('./src/testing/app'),
      $lib: dir('./src/lib'),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.spec.ts'],
    setupFiles: ['./src/testing/setup.ts'],
  },
});
