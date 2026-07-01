// eslint.config.js
import js from "@eslint/js";
import pluginVue from "eslint-plugin-vue";
import tseslint from "typescript-eslint";
import globals from "globals";

export default [
  // Base configuration files targeting specific extensions
  {
    files: ["**/*.{js,mjs,cjs,ts,vue}"],
  },

  // Base configurations
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs["flat/recommended"],

  // Connect TypeScript parser with Vue SFC parser
  {
    files: ["**/*.vue"],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser,
        sourceType: "module",
      },
    },
  },

  // Environment and rule overrides
  {
    languageOptions: {
      globals: globals.browser,
    },
    rules: {
      "vue/multi-word-component-names": "off",
      "@typescript-eslint/no-explicit-any": "warn",
    },
  },
];
