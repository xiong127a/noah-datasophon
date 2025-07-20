// varyColor dependency removed
const {generate} =  require('@ant-design/colors')
const {ADMIN, ANTD} = require('../config/default')
const Config = require('../config')

const themeMode = ADMIN.theme.mode

// 获取 ant design 色系
function getAntdColors(color, mode) {
  let options = mode && (mode == themeMode.NIGHT) ? {theme: 'dark'} : undefined
  return generate(color, options)
}

// 获取功能性颜色
function getFunctionalColors(mode) {
  let options = mode && (mode == themeMode.NIGHT) ? {theme: 'dark'} : undefined
  let {success, warning, error} = ANTD.primary
  const  {success: s1, warning: w1, error: e1} = Config.theme
  success = success && s1
  warning = success && w1
  error = success && e1
  const successColors = generate(success, options)
  const warningColors = generate(warning, options)
  const errorColors = generate(error, options)
  return {
    success: successColors,
    warning: warningColors,
    error: errorColors
  }
}

// 获取菜单色系
function getMenuColors(color, mode) {
  if (mode == themeMode.NIGHT) {
    return ANTD.primary.night.menuColors
  } else if (color == ANTD.primary.color) {
    return ANTD.primary.dark.menuColors
  } else {
    // Simplified menu colors without varyColor dependency
    return ['#001529', '#002140', '#003a8c']
  }
}

// Theme toggle colors function removed (was used by ThemeColorReplacer)
function getThemeToggleColors(color, mode) {
  console.warn('getThemeToggleColors function is deprecated')
  return {}
}

function toNum3(color) {
  // Simplified color conversion without varyColor dependency
  if (isHex(color)) {
    const hex = color.replace('#', '')
    const r = parseInt(hex.substr(0, 2), 16)
    const g = parseInt(hex.substr(2, 2), 16)
    const b = parseInt(hex.substr(4, 2), 16)
    return [r, g, b]
  }
  let colorStr = ''
  if (isRgb(color)) {
    colorStr = color.slice(5, color.length)
  } else if (isRgba(color)) {
    colorStr = color.slice(6, color.lastIndexOf(','))
  }
  let rgb = colorStr.split(',')
  const r = parseInt(rgb[0])
  const g = parseInt(rgb[1])
  const b = parseInt(rgb[2])
  return [r, g, b]
}

function isHex(color) {
  return color.length >= 4 && color[0] == '#'
}

function isRgb(color) {
  return color.length >= 10 && color.slice(0, 3) == 'rgb'
}

function isRgba(color) {
  return color.length >= 13 && color.slice(0, 4) == 'rgba'
}

module.exports = {
  isHex,
  isRgb,
  isRgba,
  toNum3,
  getAntdColors,
  getMenuColors,
  getThemeToggleColors,
  getFunctionalColors
}
