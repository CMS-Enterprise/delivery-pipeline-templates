// Stand-in for SvelteKit's $app/stores, which only exists inside a running
// SvelteKit app. Tests set the pathname before rendering to exercise the
// active-link logic in Nav.svelte.
import { writable } from 'svelte/store';

export const page = writable({ url: new URL('http://localhost/') });

export function setPathname(pathname: string) {
  page.set({ url: new URL(`http://localhost${pathname}`) });
}
