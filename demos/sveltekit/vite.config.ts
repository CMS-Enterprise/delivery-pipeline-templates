import { fileURLToPath } from 'node:url';
import { sveltekit } from '@sveltejs/kit/vite';
import adapter from '@sveltejs/adapter-node';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [sveltekit({
    adapter: adapter()
  })],
  resolve: {
    alias: {
      '#lib': fileURLToPath(new URL('./src/lib', import.meta.url))
    }
  }
});
