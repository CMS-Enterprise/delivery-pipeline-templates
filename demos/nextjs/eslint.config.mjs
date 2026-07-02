import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";

const eslintConfig = defineConfig([
  // Bring in core Next.js & Web Vitals rules
  ...nextVitals,
  // Custom rule overrides and settings
  {
    rules: {
      "no-unused-vars": "warn",
      "@next/next/no-img-element": "error",
    },
    settings: {
      react: {
        version: "detect",
      },
    },
  },

  // Files to ignore entirely from linting
  globalIgnores([".next/**", "out/**", "build/**", "next-env.d.ts"]),
]);

export default eslintConfig;
