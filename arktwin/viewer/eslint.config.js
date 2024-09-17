import eslint from '@eslint/js'
import eslintConfigPrettier from 'eslint-config-prettier'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  {
    ignores: ['**/dist/**'],
  },
  {
    rules: {
      '@typescript-eslint/no-unused-vars': [
        'warn',
        {
          argsIgnorePattern: '^_',
        },
      ],
    },
  },
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  eslintConfigPrettier,
)
