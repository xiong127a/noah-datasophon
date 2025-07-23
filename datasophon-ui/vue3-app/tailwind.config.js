/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'primary': '#0A84FF',
        'primary-dark': '#0071E3',
        'success': '#30D158',
        'danger': '#FF453A',
        'warning': '#FF9F0A',
        'info': '#64D2FF',
        'background': '#f5f5f7',
      },
      fontFamily: {
        sans: [
          '-apple-system',
          'BlinkMacSystemFont',
          'SF Pro Text',
          'SF Pro Display',
          'Helvetica Neue',
          'Arial',
          'sans-serif'
        ],
      },
      boxShadow: {
        'apple': '0 2px 8px rgba(0, 0, 0, 0.08)',
        'apple-hover': '0 8px 16px rgba(0, 0, 0, 0.12)',
      },
      backdropFilter: {
        'apple': 'blur(10px)',
      },
    },
  },
  plugins: [],
} 