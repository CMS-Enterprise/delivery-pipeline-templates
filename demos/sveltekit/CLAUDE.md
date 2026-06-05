# Project Setup

- Node.js 18+ required
- SvelteKit 2 with Svelte 4
- TypeScript strict mode enabled
- Vite 5 as the build tool

# Build & Test

- `npm install` - install dependencies
- `npm run dev` - start dev server on port 3002
- `npm run build` - production build
- `npm run preview` - preview production build on port 3002
- `npm run check` - run svelte-check for type errors
- `./run.sh` - install and start dev server

# Code Style

- Use `<script lang="ts">` in all Svelte components
- 2-space indentation
- Svelte stores for shared state (`$` auto-subscription syntax)
- Scoped `<style>` blocks in components for encapsulated CSS
- Global styles in `src/app.css`

# Architecture

- `src/routes/` - Filesystem-based routing
- `src/routes/+layout.svelte` - Root layout with Nav and footer
- `src/routes/+page.svelte` - Home page
- `src/routes/about/+page.svelte` - About page
- `src/lib/components/` - Reusable components (Nav.svelte)
- `src/app.html` - HTML shell template
- `src/app.css` - Global CSS with variables

# Workflow

- Create a feature branch: `git checkout -b feature/your-feature-name`
- Run `npm run check` to verify no type errors before committing
- Include the user prompt in commit messages
- Write tests alongside implementation

# Gotchas

- Routes use `+page.svelte` / `+layout.svelte` naming convention (SvelteKit 2)
- `<svelte:head>` for per-page title/meta tags
- CSS variables in `src/app.css` control site-wide theme; component styles are scoped
- The site brand is "RoboCare Health" (robotic healthcare company)
