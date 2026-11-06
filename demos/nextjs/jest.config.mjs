import nextJest from 'next/jest.js';

// next/jest wires up SWC, the @/ path alias from tsconfig, and CSS/asset mocks.
const createJestConfig = nextJest({ dir: './' });

export default createJestConfig({
  testEnvironment: 'jsdom',
  testMatch: ['<rootDir>/src/**/*.spec.tsx'],
  setupFilesAfterEnv: ['<rootDir>/src/testing/setup.ts'],
});
