import { fileURLToPath } from 'node:url';
import { sveltekit } from '@sveltejs/kit/vite';
import adapter from '@sveltejs/adapter-node';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [sveltekit({
    adapter: adapter(),
    // Reproducible builds: the app version defaults to the build time. Use the
    // commit time the image build supplies instead.
    ...(process.env.SOURCE_DATE_EPOCH
      ? { version: { name: process.env.SOURCE_DATE_EPOCH } }
      : {})
  })],
  resolve: {
    alias: {
      '#lib': fileURLToPath(new URL('./src/lib', import.meta.url))
    }
  }
});
