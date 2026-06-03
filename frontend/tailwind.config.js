/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: { DEFAULT: '#0D9488', fg: '#FFFFFF' },
        accent: { DEFAULT: '#EA580C', fg: '#FFFFFF' },
        background: '#F0FDFA',
        foreground: '#134E4A',
        muted: '#E8F1F4',
        bordercolor: '#99F6E4',
        destructive: '#DC2626',
      },
      fontFamily: { sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'] },
    },
  },
  plugins: [],
};
