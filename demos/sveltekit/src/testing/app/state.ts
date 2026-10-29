// Stand-in for SvelteKit's $app/state, which only exists inside a running
// SvelteKit app. In SvelteKit 3, page is a reactive object (not a store).
let _url = new URL('http://localhost/');

export const page = {
  get url() { return _url; },
  get params() { return {}; },
  get route() { return { id: null }; },
  get status() { return 200; },
  get error() { return null; },
  get data() { return {}; },
  get form() { return null; },
  get state() { return {}; },
};

export function setPathname(pathname: string) {
  _url = new URL(`http://localhost${pathname}`);
}
