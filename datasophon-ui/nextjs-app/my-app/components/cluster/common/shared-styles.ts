/**
 * 集群组件共用样式配置
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

// 对话框基础样式
export const DIALOG_STYLES = {
  // 对话框容器
  container: `
    !max-w-none !w-[min(calc(100vw-32px),1900px)] !max-h-[calc(100vh-32px)] 
    sm:!w-[min(98vw,1900px)] sm:!max-h-[calc(98vh-32px)] 
    border-0 shadow-2xl bg-white rounded-3xl 
    !fixed !top-1/2 !left-1/2 !-translate-x-1/2 !-translate-y-1/2 !m-0 
    [&>button]:hidden flex flex-col p-0 gap-0
  `,
  
  // 头部区域
  header: `
    flex items-center justify-between p-6 border-b border-gray-100
  `,
  
  // 标题样式
  title: `
    text-2xl font-semibold text-gray-900 flex items-center
  `,
  
  // 图标容器
  iconContainer: `
    w-8 h-8 rounded-lg text-white flex items-center justify-center mr-3
  `,
  
  // 关闭按钮
  closeButton: `
    text-gray-400 hover:text-gray-600
  `,
  
  // 主内容区域 - 标准统一样式（以步骤1为基准）
  content: "!max-w-none !w-[min(calc(100vw-64px),1800px)] !max-h-[min(calc(100vh-96px),900px)] sm:!w-[min(95vw,1800px)] sm:!max-h-[min(95vh,900px)] border-0 shadow-2xl bg-white rounded-3xl !fixed !top-1/2 !left-1/2 !-translate-x-1/2 !-translate-y-1/2 !m-0 [&>button]:hidden overflow-hidden",
  
  // 底部操作区域 - 统一美观样式（基于步骤1最佳样式）
  footer: `
    p-6 sm:p-8 border-t border-slate-200/50 bg-white/90 backdrop-blur-sm relative
  `,
  
  // 底部装饰光效
  footerGlow: `
    absolute inset-0 bg-gradient-to-r from-transparent via-white/80 to-transparent
  `,
  
  // 顶部分割线光效  
  footerTopLine: `
    absolute top-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/60 to-transparent
  `,
  
  // 底部内容容器
  footerContent: `
    flex justify-between items-center relative z-10
  `,
  
  // 按钮容器
  buttonContainer: `
    flex items-center justify-between
  `
} as const

// 按钮样式配置
export const BUTTON_STYLES = {
  // 主要按钮
  primary: `
    px-6 bg-blue-600 hover:bg-blue-700 text-white
  `,
  
  // 次要按钮
  secondary: `
    px-6
  `,
  
  // 取消按钮
  cancel: `
    px-6 border border-gray-200 hover:border-gray-300
  `,
  
  // 上一步按钮
  previous: `
    flex items-center px-5 py-2.5 bg-gray-50 hover:bg-gray-100 
    border border-gray-200 hover:border-gray-300 rounded-xl 
    text-sm font-medium text-gray-700 transition-all duration-200 
    shadow-sm hover:shadow-md
  `,
  
  // 下一步按钮
  next: `
    flex items-center px-6 py-2.5 rounded-xl text-sm font-medium 
    transition-all duration-200 shadow-md hover:shadow-lg
  `,
  
  // 下一步按钮 - 启用状态
  nextEnabled: `
    bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 
    hover:to-blue-700 text-white transform hover:scale-105
  `,
  
  // 下一步按钮 - 禁用状态
  nextDisabled: `
    bg-gray-200 text-gray-400 cursor-not-allowed
  `,
  
  // 加载状态
  loading: `
    bg-gray-200 text-gray-400 cursor-not-allowed
  `
} as const

// 卡片样式配置
export const CARD_STYLES = {
  // 基础卡片
  base: `
    border border-gray-200 rounded-xl bg-white shadow-sm
  `,
  
  // 信息卡片 - 蓝色主题
  info: `
    border-blue-200 bg-blue-50/50
  `,
  
  // 成功卡片 - 绿色主题
  success: `
    border-green-200 bg-green-50/50
  `,
  
  // 警告卡片 - 黄色主题
  warning: `
    border-amber-200 bg-amber-50/50
  `,
  
  // 错误卡片 - 红色主题
  error: `
    border-red-200 bg-red-50/50
  `,
  
  // 卡片头部
  header: `
    pb-4
  `,
  
  // 卡片标题
  title: `
    text-lg flex items-center
  `,
  
  // 卡片内容
  content: `
    space-y-4
  `
} as const

// 表格样式配置
export const TABLE_STYLES = {
  // 表格容器
  container: `
    flex-1 flex flex-col min-h-0
  `,
  
  // 表格头部
  header: `
    pb-3 flex-shrink-0
  `,
  
  // 搜索框
  searchInput: `
    h-9 w-56 pl-10 pr-4 py-2 text-sm font-medium border-2 border-gray-200/60 
    rounded-2xl bg-white/95 backdrop-blur-md shadow-sm hover:shadow-xl 
    focus:outline-none focus:ring-3 focus:ring-blue-400/25 focus:border-blue-400 
    hover:border-gray-300/80 focus:bg-white placeholder:text-gray-400 
    transition-all duration-300 ease-out
  `,
  
  // 筛选选择框
  selectTrigger: `
    h-9 min-w-[90px] border-2 border-gray-200/60 rounded-2xl bg-white/95 
    backdrop-blur-md shadow-sm hover:shadow-xl focus:ring-3 focus:ring-blue-400/25 
    focus:border-blue-400 hover:border-gray-300/80 transition-all duration-300 ease-out
  `,
  
  // 表格主体
  body: `
    flex-1 overflow-y-auto min-h-0 max-h-[calc(100vh-380px)] 
    scrollbar-thin scrollbar-track-transparent 
    scrollbar-thumb-gray-200 hover:scrollbar-thumb-gray-300
  `,
  
  // 分页容器
  pagination: `
    bg-white backdrop-blur-md border-t border-gray-200/80 p-3 flex-shrink-0 shadow-lg
  `
} as const

// 主机行样式配置
export const HOST_STYLES = {
  // 主机卡片容器
  container: `
    group relative transform transition-all duration-200 ease-out hover:scale-[1.01]
  `,
  
  // 主机卡片
  card: `
    relative rounded-xl border transition-all duration-300 cursor-pointer overflow-hidden shadow-sm
  `,
  
  // 选中状态
  selected: `
    border-blue-300 bg-gradient-to-br from-blue-50 via-white to-blue-50/50 
    shadow-lg shadow-blue-100/50
  `,
  
  // 未选中状态
  unselected: `
    border-gray-200 bg-white hover:border-gray-300 hover:shadow-md
  `,
  
  // 选中指示条
  selectedIndicator: `
    absolute left-0 top-0 bottom-0 w-1 transition-all duration-300 
    bg-gradient-to-b from-blue-500 to-blue-600
  `,
  
  // 选择框
  checkbox: `
    w-3 h-3 rounded-full border-2 transition-all duration-200 flex items-center justify-center
  `,
  
  // 选中的选择框
  checkboxSelected: `
    border-blue-500 bg-blue-500
  `,
  
  // 未选中的选择框
  checkboxUnselected: `
    border-gray-300 group-hover:border-blue-300
  `
} as const

// Badge样式配置
export const BADGE_STYLES = {
  // 状态Badge
  status: {
    ready: `
      bg-emerald-100 text-emerald-800 border border-emerald-200
    `,
    notReady: `
      bg-rose-100 text-rose-800 border border-rose-200
    `,
    unknown: `
      bg-gray-100 text-gray-800 border border-gray-200
    `
  },
  
  // 管理状态Badge
  management: {
    managed: `
      bg-emerald-50 text-emerald-700 border-emerald-200
    `,
    unmanaged: `
      bg-rose-50 text-rose-700 border-rose-200
    `,
    configuring: `
      bg-amber-50 text-amber-700 border-amber-200
    `
  },
  
  // 角色Badge
  role: {
    none: `
      bg-gray-100 text-gray-600
    `,
    controlPlane: `
      bg-purple-100 text-purple-700
    `,
    worker: `
      bg-blue-100 text-blue-700
    `
  },
  
  // 基础Badge样式
  base: `
    inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium 
    transition-all duration-200
  `
} as const

// 动画效果
export const ANIMATIONS = {
  // 淡入效果
  fadeIn: `
    animate-in fade-in duration-300
  `,
  
  // 滑入效果
  slideIn: `
    animate-in slide-in-from-bottom-4 duration-300
  `,
  
  // 缩放效果
  scaleIn: `
    animate-in zoom-in-95 duration-200
  `,
  
  // 旋转加载
  spin: `
    animate-spin
  `,
  
  // 脉冲效果
  pulse: `
    animate-pulse
  `
} as const

// 响应式断点
export const BREAKPOINTS = {
  sm: '640px',
  md: '768px', 
  lg: '1024px',
  xl: '1280px',
  '2xl': '1536px'
} as const

// 颜色主题
export const COLORS = {
  // 主色调
  primary: {
    50: '#eff6ff',
    100: '#dbeafe', 
    500: '#3b82f6',
    600: '#2563eb',
    700: '#1d4ed8'
  },
  
  // 成功色
  success: {
    50: '#f0fdf4',
    100: '#dcfce7',
    500: '#22c55e',
    600: '#16a34a',
    700: '#15803d'
  },
  
  // 警告色
  warning: {
    50: '#fffbeb',
    100: '#fef3c7',
    500: '#f59e0b',
    600: '#d97706',
    700: '#b45309'
  },
  
  // 错误色
  error: {
    50: '#fef2f2',
    100: '#fee2e2',
    500: '#ef4444',
    600: '#dc2626', 
    700: '#b91c1c'
  },
  
  // 中性色
  gray: {
    50: '#f9fafb',
    100: '#f3f4f6',
    200: '#e5e7eb',
    300: '#d1d5db',
    400: '#9ca3af',
    500: '#6b7280',
    600: '#4b5563',
    700: '#374151',
    800: '#1f2937',
    900: '#111827'
  }
} as const

// 间距配置
export const SPACING = {
  xs: '0.25rem',   // 1
  sm: '0.5rem',    // 2
  md: '0.75rem',   // 3
  lg: '1rem',      // 4
  xl: '1.25rem',   // 5
  '2xl': '1.5rem', // 6
  '3xl': '2rem',   // 8
  '4xl': '3rem',   // 12
} as const

// 阴影配置
export const SHADOWS = {
  sm: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
  md: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
  lg: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
  xl: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
  '2xl': '0 25px 50px -12px rgb(0 0 0 / 0.25)'
} as const

// 实用工具函数
export const cn = (...classes: (string | undefined | false)[]) => {
  return classes.filter(Boolean).join(' ')
}
