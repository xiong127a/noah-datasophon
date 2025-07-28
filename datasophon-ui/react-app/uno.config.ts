import { defineConfig, presetUno, presetIcons, presetAttributify, transformerVariantGroup, transformerDirectives } from 'unocss';

export default defineConfig({
  shortcuts: {
    // 定义常用的快捷组合
    'flex-center': 'flex items-center justify-center',
    'flex-between': 'flex items-center justify-between',
    'btn': 'px-4 py-2 rounded-lg transition-all duration-200',
    'btn-primary': 'btn bg-blue-600 text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500',
    'btn-secondary': 'btn bg-gray-200 text-gray-800 hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-400',
    'input-base': 'w-full px-3 py-2 rounded-lg focus:outline-none',
    'input-primary': 'input-base bg-white/10 backdrop-filter backdrop-blur-md text-white border-0 focus:ring-2 focus:ring-blue-400',
    'card': 'bg-white rounded-lg shadow-lg p-6',
    'card-glass': 'bg-white/10 backdrop-filter backdrop-blur-lg rounded-3xl shadow-2xl overflow-hidden',
    'text-gradient': 'bg-clip-text text-transparent bg-gradient-to-r from-blue-500 to-indigo-600',
    // 高科技渐变文本
    'text-gradient-blue-sky': 'bg-clip-text text-transparent bg-gradient-to-r from-blue-300 to-sky-300',
    // 网格背景
    'bg-grid-white/10': 'bg-[url("data:image/svg+xml,%3csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 32 32\' width=\'32\' height=\'32\' fill=\'none\' stroke=\'%23FFFFFF10\'%3e%3cpath d=\'M0 .5H31.5V32\'/%3e%3c/svg%3e")]',
    'bg-grid-white/20': 'bg-[url("data:image/svg+xml,%3csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 32 32\' width=\'32\' height=\'32\' fill=\'none\' stroke=\'%23FFFFFF20\'%3e%3cpath d=\'M0 .5H31.5V32\'/%3e%3c/svg%3e")]',
    'bg-grid-white/5': 'bg-[url("data:image/svg+xml,%3csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 32 32\' width=\'32\' height=\'32\' fill=\'none\' stroke=\'%23FFFFFF05\'%3e%3cpath d=\'M0 .5H31.5V32\'/%3e%3c/svg%3e")]',
    'bg-grid-white/[0.03]': 'bg-[url("data:image/svg+xml,%3csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 32 32\' width=\'32\' height=\'32\' fill=\'none\' stroke=\'%23FFFFFF05\'%3e%3cpath d=\'M0 .5H31.5V32\'/%3e%3c/svg%3e")]'
  },
  theme: {
    colors: {
      'apple-blue': {
        50: '#f0f9ff',
        100: '#e0f2fe',
        200: '#bae6fd',
        300: '#7dd3fc',
        400: '#38bdf8',
        500: '#0ea5e9',
        600: '#0284c7',
        700: '#0369a1',
        800: '#075985',
        900: '#0c4a6e',
      },
    },
    boxShadow: {
      'apple': '0 0 0 1px rgba(0, 0, 0, 0.05), 0 1px 2px 0 rgba(0, 0, 0, 0.05)',
      'apple-md': '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
      'apple-lg': '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
      'apple-xl': '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
      'apple-2xl': '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
    },
    dropShadow: {
      'blue-sm': '0 0 5px rgba(59, 130, 246, 0.3)',
      'blue-lg': '0 0 10px rgba(59, 130, 246, 0.5)',
    },
    fontFamily: {
      sans: 'system-ui, -apple-system, Segoe UI, Roboto, Helvetica Neue, Arial, sans-serif',
      apple: '-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue, Arial, sans-serif',
    },
    extend: {
      animation: {
        'spin-slow': 'spin 15s linear infinite',
        'reverse-spin-slow': 'reverse-spin 20s linear infinite',
        'pulse-slow': 'pulse 4s ease-in-out infinite',
      },
    }
  },
  presets: [
    presetUno(),
    presetIcons({
      scale: 1.2,
      extraProperties: {
        'display': 'inline-block',
        'vertical-align': 'middle',
      },
    }),
    presetAttributify(),
  ],
  transformers: [
    transformerDirectives(),
    transformerVariantGroup(),
  ],
  safelist: [
    // 动画类
    'animate-spin', 'animate-blob', 'animation-delay-2000', 'animation-delay-4000',
    'animate-pulse', 'animate-shimmer', 'animate-spin-slow', 'animate-reverse-spin-slow', 'animate-pulse-slow',
    // 高亮和发光效果
    'drop-shadow-blue-sm', 'drop-shadow-blue-lg', 'glow-blue-sm', 'glow-blue-md',
    // 交互状态类
    'scale-x-0', 'scale-x-100',
  ],
  rules: [
    // 自定义背景模糊
    ['backdrop-blur-apple', { 'backdrop-filter': 'blur(20px)', '-webkit-backdrop-filter': 'blur(20px)' }],
    
    // 动画
    ['animate-blob', {
      'animation': 'blob 7s infinite',
    }],
    ['animate-shimmer', {
      'animation': 'shimmer 2s infinite',
    }],
    ['animate-pulse-slow', {
      'animation': 'pulse 4s ease-in-out infinite',
    }],
    
    // 径向渐变背景
    ['bg-gradient-radial', {
      'background-image': 'radial-gradient(var(--un-gradient-stops))',
    }],
    
    // 发光效果
    ['glow-blue-sm', { 'box-shadow': '0 0 5px 0px rgba(59, 130, 246, 0.5), 0 0 20px 0px rgba(59, 130, 246, 0.3)' }],
    ['glow-blue-md', { 'box-shadow': '0 0 10px 0px rgba(59, 130, 246, 0.6), 0 0 30px 0px rgba(59, 130, 246, 0.4)' }],
  ],
  preflights: [
    {
      // 添加额外的关键帧动画
      getCSS: () => `
        @keyframes shimmer {
          0% {
            transform: translateX(-150%);
          }
          100% {
            transform: translateX(150%);
          }
        }
        
        @keyframes pulse {
          0%, 100% {
            opacity: 0.8;
          }
          50% {
            opacity: 0.4;
          }
        }
        
        @keyframes reverse-spin {
          from {
            transform: rotate(360deg);
          }
          to {
            transform: rotate(0deg);
          }
        }

        @keyframes float {
          0%, 100% {
            transform: translateY(0);
          }
          50% {
            transform: translateY(-10px);
          }
        }

        /* 全局CSS，用于支持高科技效果 */
        body {
          background-color: #0f172a;
          color: #fff;
        }
        
        .tech-btn:hover .glow-blue-sm {
          box-shadow: 0 0 10px 0px rgba(59, 130, 246, 0.7), 0 0 30px 0px rgba(59, 130, 246, 0.5);
          transition: box-shadow 0.3s ease;
        }
      `
    }
  ]
}); 