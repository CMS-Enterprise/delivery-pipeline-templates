// adapter-auto emits nothing runnable outside a recognised host, so the
// container image needs adapter-node's build/index.js server.
import adapter from '@sveltejs/adapter-node';

/** @type {import('@sveltejs/kit').Config} */
const config = {
  kit: {
    adapter: adapter()
  }
};

export default config;
