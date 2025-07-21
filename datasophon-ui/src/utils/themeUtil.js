const {theme} = require('../config')
const {getMenuColors, getAntdColors, getFunctionalColors} = require('../utils/colors')
const {ANTD} = require('../config/default')

// Theme color changing functionality removed
function changeThemeColor(newColor, $theme) {
  console.warn('Theme color changing has been disabled')
  return Promise.resolve()
}

function modifyVars(color) {
  let _color = color || theme.color
  const palettes = getAntdColors(_color, theme.mode)
  const menuColors = getMenuColors(_color, theme.mode)
  const {success, warning, error} = getFunctionalColors(theme.mode)
  const primary = palettes[5]
  return {
    'primary-color': '#007AFF',  // 苹果蓝色
    'primary-1': palettes[0],
    'primary-2': palettes[1],
    'primary-3': palettes[2],
    'primary-4': palettes[3],
    'primary-5': '#5AC8FA', // 苹果浅蓝色
    'primary-6': '#007AFF', // 苹果蓝色
    'primary-7': '#147AFC', // 深一点的蓝色
    'primary-8': '#0056CA', // 更深的蓝色
    'primary-9': palettes[8],
    'primary-10': palettes[9],
    'info-color': '#007AFF',  // 苹果蓝色
    'success-color': '#34C759',  // 苹果绿色
    'warning-color': '#FF9500',  // 苹果橙色
    'error-color': '#FF3B30',  // 苹果红色
    'alert-info-bg-color': 'rgba(0, 122, 255, 0.1)',
    'alert-info-border-color': 'rgba(0, 122, 255, 0.2)',
    'alert-success-bg-color': 'rgba(52, 199, 89, 0.1)',
    'alert-success-border-color': 'rgba(52, 199, 89, 0.2)',
    'alert-warning-bg-color': 'rgba(255, 149, 0, 0.1)',
    'alert-warning-border-color': 'rgba(255, 149, 0, 0.2)',
    'alert-error-bg-color': 'rgba(255, 59, 48, 0.1)',
    'alert-error-border-color': 'rgba(255, 59, 48, 0.2)',
    'processing-color': '#007AFF',
    'menu-dark-submenu-bg': menuColors[0],
    'layout-header-background': menuColors[1],
    'layout-trigger-background': menuColors[2],
    'btn-danger-bg': '#FF3B30',
    'btn-danger-border': '#FF3B30',
    
    // 苹果风格设计变量
    'border-radius-base': '8px',  // 基础圆角
    'border-radius-sm': '6px',    // 小号圆角
    'input-height-base': '38px',  // 输入框高度
    'input-padding-horizontal': '16px', // 输入框水平内边距
    'select-dropdown-height': '38px',  // 下拉选项高度
    'select-item-selected-bg': 'rgba(0, 122, 255, 0.1)', // 选中项背景色
    'select-item-active-bg': 'rgba(0, 122, 255, 0.05)',  // 激活项背景色
    'checkbox-size': '18px',  // 复选框大小
    'checkbox-border-radius': '4px', // 复选框圆角
    'switch-min-width': '44px', // 开关宽度
    'switch-height': '24px',    // 开关高度
    'form-item-margin-bottom': '24px', // 表单项底部间距
    'btn-border-radius-base': '8px', // 按钮圆角
    'btn-height-base': '38px',       // 按钮高度
    'btn-padding-base': '0 20px',    // 按钮内边距
    'btn-font-weight': '500',        // 按钮字体粗细
    'animation-duration-slow': '.3s', // 动画持续时间
    'ease-in-out': 'cubic-bezier(0.25, 0.1, 0.25, 1)', // 动画曲线
    
    // 苹果设计文本颜色系统
    'text-color': '#1D1D1F',     // 主文本色
    'text-color-secondary': '#8E8E93', // 次要文本色
    'heading-color': '#1D1D1F',  // 标题文本色
    'disabled-color': '#AEAEB2',  // 禁用状态色
    'border-color-base': '#D1D1D6',    // 边框颜色
    'divider-color': 'rgba(60, 60, 67, 0.08)', // 分割线颜色
    'box-shadow-base': '0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04)',
    ...ANTD.theme[theme.mode]
  }
}

function loadLocalTheme(localSetting) {
  // Theme loading functionality disabled
  console.warn('Theme loading has been disabled')
}

/**
 * 获取本地保存的配置
 * @param load {boolean} 是否加载配置中的主题
 * @returns {Object}
 */
function getLocalSetting(loadTheme) {
  let localSetting = {}
  try {
    const localSettingStr = localStorage.getItem(process.env.VUE_APP_SETTING_KEY)
    localSetting = JSON.parse(localSettingStr)
  } catch (e) {
    console.error(e)
  }
  if (loadTheme) {
    loadLocalTheme(localSetting)
  }
  return localSetting
}

module.exports = {
  changeThemeColor,
  modifyVars,
  loadLocalTheme,
  getLocalSetting
}
