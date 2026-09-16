/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          primary: 'var(--brand-primary, #2563eb)',
          secondary: 'var(--brand-secondary, #0f172a)',
          accent: 'var(--brand-accent, #3b82f6)',
          surface: 'var(--brand-surface, #ffffff)',
          background: 'var(--brand-bg, #f8fafc)',
        }
      }
    },
  },
  plugins: [],
}