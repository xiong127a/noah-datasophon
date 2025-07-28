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
    'menu-active': 'bg-primary-50/80 text-primary-600 font-medium',
    'menu-hover': 'bg-black/3 text-gray-800',
    'apple-button': 'bg-gradient-to-b from-white/70 to-white/50 backdrop-blur-sm rounded-full border border-white/20 shadow-sm hover:shadow-md active:scale-98 transition-all duration-150',
    'apple-menu': 'animate-fade-in-fast origin-top backdrop-blur-xl bg-white/90 border border-gray-100/20 shadow-apple rounded-xl overflow-hidden',
    'menu-item': 'flex items-center px-3 py-2 rounded-lg transition-all duration-150',
    'apple-header': 'backdrop-blur-xl bg-white/90 z-50 shadow-subtle border-b border-gray-100/20',
    'action-button': 'w-9 h-9 flex-center rounded-lg bg-black/3 hover:bg-black/5 transition-apple',
  },
  // 主题配置
  theme: {
    colors: {
      // 苹果风格的颜色
      primary: {
        DEFAULT: '#0071e3',
        50: '#e9f4ff',
        100: '#c7e3ff',
        200: '#95c9ff',
        300: '#64b0ff',
        400: '#3498ff',
        500: '#0071e3',
        600: '#0058c2',
        700: '#0040a1',
        800: '#002d81',
        900: '#00205d',
      },
      apple: {
        blue: '#0071e3',
        purple: '#5d5cde',
        pink: '#ff375f',
        orange: '#ff9f0a',
        yellow: '#ffd60a',
        green: '#32d74b',
        gray: '#86868b',
        dark: '#1d1d1f',
        light: '#f5f5f7',
      }
    },
    boxShadow: {
      'subtle': '0 1px 2px rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02)',
      'elevated': '0 4px 8px rgba(0, 0, 0, 0.04), 0 0 2px rgba(0, 0, 0, 0.02)',
      'prominent': '0 8px 16px rgba(0, 0, 0, 0.08), 0 4px 8px rgba(0, 0, 0, 0.06)',
      'apple': '0 2px 10px rgba(0, 0, 0, 0.05), 0 0 1px rgba(0, 0, 0, 0.05)',
      'menu': '0 8px 20px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06)',
    },
    extend: {
      animation: {
        'spin-slow': 'spin 15s linear infinite',
        'reverse-spin-slow': 'reverse-spin 20s linear infinite',
        'pulse-slow': 'pulse 4s ease-in-out infinite',
        'float': 'float 4s ease-in-out infinite',
        'fade-in-down': 'fadeInDown 0.3s cubic-bezier(0.25, 1, 0.5, 1) forwards',
        'fade-out-up': 'fadeOutUp 0.3s cubic-bezier(0.25, 1, 0.5, 1) forwards',
        'fade-in-fast': 'fadeInDown 0.15s cubic-bezier(0.25, 1, 0.5, 1) forwards',
        'fade-out-fast': 'fadeOutUp 0.15s cubic-bezier(0.25, 1, 0.5, 1) forwards',
        'scale-in': 'scaleIn 0.15s cubic-bezier(0.34, 1.56, 0.64, 1) forwards',
        'scale-out': 'scaleOut 0.12s cubic-bezier(0.34, 0.96, 0.64, 1) forwards',
        'blur-in': 'blurIn 0.15s cubic-bezier(0.25, 1, 0.5, 1) forwards',
      },
      keyframes: {
        'fadeInDown': {
          '0%': { opacity: '0', transform: 'translateY(-10px) scale(0.98)' },
          '100%': { opacity: '1', transform: 'translateY(0) scale(1)' },
        },
        'fadeOutUp': {
          '0%': { opacity: '1', transform: 'translateY(0) scale(1)' },
          '100%': { opacity: '0', transform: 'translateY(-10px) scale(0.98)' },
        },
        'scaleIn': {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        'scaleOut': {
          '0%': { opacity: '1', transform: 'scale(1)' },
          '100%': { opacity: '0', transform: 'scale(0.95)' },
        },
        'blurIn': {
          '0%': { opacity: '0', filter: 'blur(4px)' },
          '100%': { opacity: '1', filter: 'blur(0)' },
        },
      },
      transitionTimingFunction: {
        'apple': 'cubic-bezier(0.25, 1, 0.5, 1)',
        'apple-spring': 'cubic-bezier(0.34, 1.56, 0.64, 1)',
        'apple-out': 'cubic-bezier(0.34, 0.96, 0.64, 1)',
      },
      scale: {
        '98': '0.98',
        '102': '1.02',
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
      'backdrop-blur-menu',
      { 'backdrop-filter': 'blur(10px) saturate(180%)', '-webkit-backdrop-filter': 'blur(10px) saturate(180%)' },
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
    [
      'glass-morphism-light',
      { 
        'background': 'rgba(255, 255, 255, 0.7)',
        'backdrop-filter': 'blur(10px)',
        '-webkit-backdrop-filter': 'blur(10px)',
        'border': '1px solid rgba(255, 255, 255, 0.2)'
      },
    ],
    [
      'glass-morphism-menu',
      { 
        'background': 'rgba(255, 255, 255, 0.95)',
        'backdrop-filter': 'blur(10px)',
        '-webkit-backdrop-filter': 'blur(10px)',
        'border': '1px solid rgba(255, 255, 255, 0.3)',
        'box-shadow': '0 8px 32px rgba(0, 0, 0, 0.1)'
      },
    ],
    [
      'transition-apple',
      { 'transition': 'all 0.2s cubic-bezier(0.25, 1, 0.5, 1)' },
    ],
    [
      'transition-apple-spring',
      { 'transition': 'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)' },
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
    'i-carbon-settings',
    'i-carbon-notification',
    'i-carbon-notification-new',
    'i-carbon-notification-off',
    'i-carbon-bare-metal-server',
    'i-carbon-server-rack',
    'i-carbon-table',
    'i-carbon-help',
    'i-carbon-group',
    'i-carbon-user-profile',
    'i-carbon-rack-server',
    'i-carbon-tag',
    'i-carbon-document',
    'i-carbon-list',
    'i-carbon-data-base',
    'i-carbon-network-4',
    'i-carbon-cloud',
    'i-carbon-history',
    'i-carbon-chevron-down',
    'i-carbon-close',
    'i-carbon-checkmark',
    'i-carbon-search',
    'i-carbon-filter',
    'i-carbon-add',
    'i-carbon-view',
    'i-carbon-edit',
    'i-carbon-trash-can',
    'i-carbon-chevron-left',
    'i-carbon-chevron-right',
    'i-carbon-chart-line',
    'i-carbon-application',
    // 动画类
    'animate-spin', 
    'animate-pulse', 
    'animate-spin-slow', 
    'animate-reverse-spin-slow', 
    'animate-pulse-slow',
    'animate-float',
    'animate-fade-in-down',
    'animate-fade-out-up',
    'animate-fade-in-fast',
    'animate-fade-out-fast',
    'animate-scale-in',
    'animate-scale-out',
    'animate-blur-in',
  ],
}); 