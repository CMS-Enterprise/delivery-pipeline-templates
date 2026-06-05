# Project Setup

- Node.js 18+ required
- Vue 3 with Composition API (`<script setup>`)
- Vite 5 for dev server and builds
- TypeScript strict mode enabled

# Build & Test

- `npm install` - install dependencies
- `npm run dev` - start dev server on port 3001
- `npm run build` - type-check and build for production
- `npm run preview` - preview production build on port 3001
- `./run.sh` - install and start dev server

# Code Style

- ES modules (import/export)
- Use `<script setup lang="ts">` in all single-file components
- 2-space indentation
- Define props with `defineProps<{}>()` and emits with `defineEmits<{}>()`
- Keep template, script, and style order consistent in .vue files

# Architecture

- `src/App.vue` - Root component with navigation state
- `src/components/` - Reusable UI components (NavBar, etc.)
- `src/views/` - Page-level view components (HomeView, AboutView)
- `src/assets/main.css` - Global styles with CSS variables
- `index.html` - Entry point (Vite SPA)
- No vue-router; navigation handled via reactive state in App.vue

# Workflow

- Create a feature branch: `git checkout -b feature/your-feature-name`
- Run `npm run build` to verify TypeScript and template errors before committing
- Include the user prompt in commit messages
- Write tests alongside implementation

# Gotchas

- No vue-router installed — page switching uses a `currentPage` ref in App.vue
- CSS variables defined in `src/assets/main.css` control the site-wide theme
- The site brand is "RoboCare Health" (robotic healthcare company)
