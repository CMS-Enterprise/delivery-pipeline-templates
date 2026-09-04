// Stand-in for SvelteKit's $app/paths. The real resolve() prepends the
// configured base path; there is none here, so identity is faithful.
export function resolve(path: string) {
  return path;
}

export const base = '';
