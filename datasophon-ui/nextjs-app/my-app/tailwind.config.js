/**
 * @type {import('tailwindcss').Config}
 */
module.exports = {
  theme: {
    extend: {
      animation: {
        'gradient-x': 'gradient-x 5s ease infinite',
        'float': 'float 6s ease-in-out infinite',
        'pulse-slow': 'pulse 8s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'shimmer': 'shimmer 3s linear infinite',
        'spin-slow': 'spin 8s linear infinite',
        'scale-in': 'scale-in 0.5s ease-out',
        'shake': 'shake 0.82s cubic-bezier(.36,.07,.19,.97) both',
        'scan-vertical': 'scan-vertical 15s linear infinite',
        'scan-horizontal': 'scan-horizontal 20s linear infinite',
        'radar-beam': 'radar-beam 8s linear infinite',
        'matrix-code': 'matrix-code 10s linear infinite',
        'matrix-code-slow': 'matrix-code 15s linear infinite',
        'border-flow': 'border-flow 5s linear infinite',
        'scan-btn': 'scan-btn 2s linear infinite',
        'fade-in': 'fade-in 0.6s ease-out',
        'fade-in-delay': 'fade-in 0.8s ease-out 0.2s both',
      },
      keyframes: {
        'gradient-x': {
          '0%, 100%': { 
            'background-size': '200% 200%',
            'background-position': 'left center'
          },
          '50%': {
            'background-size': '200% 200%',
            'background-position': 'right center'
          },
        },
        'float': {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-20px)' },
        },
        'shimmer': {
          '0%': { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(100%)' },
        },
        'scale-in': {
          '0%': { transform: 'scale(0.9)', opacity: '0' },
          '100%': { transform: 'scale(1)', opacity: '1' },
        },
        'shake': {
          '10%, 90%': { transform: 'translate3d(-1px, 0, 0)' },
          '20%, 80%': { transform: 'translate3d(2px, 0, 0)' },
          '30%, 50%, 70%': { transform: 'translate3d(-4px, 0, 0)' },
          '40%, 60%': { transform: 'translate3d(4px, 0, 0)' },
        },
        'scan-vertical': {
          '0%': { top: '0%', opacity: '0' },
          '5%': { opacity: '0.8' },
          '95%': { opacity: '0.8' },
          '100%': { top: '100%', opacity: '0' },
        },
        'scan-horizontal': {
          '0%': { left: '0%', opacity: '0' },
          '5%': { opacity: '0.8' },
          '95%': { opacity: '0.8' },
          '100%': { left: '100%', opacity: '0' },
        },
        'radar-beam': {
          '0%': { transform: 'rotate(0deg)', background: 'linear-gradient(90deg, rgba(56,189,248,0) 0%, rgba(56,189,248,0.3) 50%, rgba(56,189,248,0) 100%)' },
          '100%': { transform: 'rotate(360deg)', background: 'linear-gradient(90deg, rgba(56,189,248,0) 0%, rgba(56,189,248,0.3) 50%, rgba(56,189,248,0) 100%)' },
        },
        'matrix-code': {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(100%)' },
        },
        'border-flow': {
          '0%, 100%': { transform: 'rotate(0deg)' },
          '50%': { transform: 'rotate(180deg)' },
        },
        'scan-btn': {
          '0%': { transform: 'translateX(0%)' },
          '100%': { transform: 'translateX(200%)' },
        },
        'fade-in': {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [
    function({ addUtilities }) {
      const newUtilities = {
        '.perspective-1000': {
          perspective: '1000px',
        },
        '.preserve-3d': {
          transformStyle: 'preserve-3d',
        },
        '.backface-hidden': {
          backfaceVisibility: 'hidden',
        },
        '.scale-98': {
          transform: 'scale(0.98)',
        },
        '.scale-102': {
          transform: 'scale(1.02)',
        },
      }
      addUtilities(newUtilities)
    },
  ],
} 