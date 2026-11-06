import { defineConfig, devices } from '@playwright/test';

// BASE_URL is the app under test: the sidecar on localhost in the Image Tests
// stage, or the dev ingress after deployment.
export default defineConfig({
  testDir: './tests',
  retries: process.env.CI ? 1 : 0,
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
