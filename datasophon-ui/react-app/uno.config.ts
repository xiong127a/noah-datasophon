import { defineConfig, presetUno, presetIcons, presetAttributify } from 'unocss';

export default defineConfig({
  presets: [
    presetUno(),
    presetIcons({
      scale: 1.2,
      warn: true,
      extraProperties: {
        'display': 'inline-block',
        'vertical-align': 'middle',
      },
    }),
    presetAttributify(),
  ],
  // 快捷方式
  shortcuts: {
    'flex-center': 'flex items-center justify-center',
    'grid-center': 'grid place-items-center',
    'absolute-center': 'absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2',
    'flex-between': 'flex items-center justify-between',
    'btn': 'px-4 py-2 rounded-lg transition-all duration-200',
    'btn-primary': 'btn bg-blue-600 text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500',
    'btn-secondary': 'btn bg-gray-200 text-gray-800 hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-400',
    'card': 'bg-white rounded-lg shadow-lg p-6',
    'card-glass': 'bg-white/10 backdrop-filter backdrop-blur-lg rounded-3xl shadow-2xl overflow-hidden',
    'text-gradient': 'bg-clip-text text-transparent bg-gradient-to-r from-blue-500 to-indigo-600',
  },
  // 主题配置
  theme: {
    colors: {
      // 自定义主题色
      primary: {
        DEFAULT: '#007aff',
        50: '#e0f0ff',
        100: '#b8ddff',
        200: '#8cc9ff',
        300: '#59b5ff',
        400: '#29a1ff',
        500: '#007aff',
        600: '#0062cc',
        700: '#004999',
        800: '#003166',
        900: '#001933',
      },
    },
    boxShadow: {
      'subtle': '0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 3px rgba(0, 0, 0, 0.03)',
      'elevated': '0 4px 16px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04)',
      'prominent': '0 8px 32px rgba(0, 0, 0, 0.12), 0 4px 16px rgba(0, 0, 0, 0.06)',
    },
    extend: {
      animation: {
        'spin-slow': 'spin 15s linear infinite',
        'reverse-spin-slow': 'reverse-spin 20s linear infinite',
        'pulse-slow': 'pulse 4s ease-in-out infinite',
        'float': 'float 4s ease-in-out infinite',
      },
      transitionTimingFunction: {
        'apple': 'cubic-bezier(0.25, 1, 0.5, 1)',
        'apple-spring': 'cubic-bezier(0.34, 1.56, 0.64, 1)',
      },
    },
  },
  // 自定义规则
  rules: [
    [
      'backdrop-blur-apple',
      { 'backdrop-filter': 'blur(20px) saturate(180%)', '-webkit-backdrop-filter': 'blur(20px) saturate(180%)' },
    ],
    [
      'bg-gradient-radial',
      { 'background-image': 'radial-gradient(var(--un-gradient-stops))' },
    ],
    [
      'glass-morphism',
      { 
        'background': 'rgba(255, 255, 255, 0.05)',
        'backdrop-filter': 'blur(16px)',
        '-webkit-backdrop-filter': 'blur(16px)',
        'border': '1px solid rgba(255, 255, 255, 0.1)'
      },
    ],
  ],
  safelist: [
    // 图标类
    'i-carbon-dashboard',
    'i-carbon-home',
    'i-carbon-cloud-services',
    'i-carbon-user-admin',
    'i-carbon-user',
    'i-carbon-logout',
    // 动画类
    'animate-spin', 
    'animate-pulse', 
    'animate-spin-slow', 
    'animate-reverse-spin-slow', 
    'animate-pulse-slow',
    'animate-float',
  ],
}); 