# Project Setup

- Node.js 18+ required
- Next.js 14 with App Router
- TypeScript strict mode enabled

# Build & Test

- `npm install` - install dependencies
- `npm run dev` - start dev server on port 3000
- `npm run build` - production build
- `npm run start` - serve production build on port 3000
- `./run.sh` - install and start dev server

# Code Style

- ES modules (import/export)
- React Server Components by default (use 'use client' only when needed)
- 2-space indentation
- Single quotes for imports, JSX uses double quotes
- Prefer named exports for page components

# Architecture

- `src/app/` - App Router pages and layouts
- `src/app/globals.css` - Global styles with CSS variables
- `src/app/layout.tsx` - Root layout with nav and footer
- Pages are directories with `page.tsx` files (e.g., `src/app/about/page.tsx`)

# Workflow

- Create a feature branch: `git checkout -b feature/your-feature-name`
- Run `npm run build` to verify no TypeScript errors before committing
- Include the user prompt in commit messages
- Write tests alongside implementation

# Gotchas

- No `pages/` directory — this project uses the App Router exclusively
- CSS variables defined in `globals.css` control the site-wide theme
- The site brand is "RoboCare Health" (robotic healthcare company)
