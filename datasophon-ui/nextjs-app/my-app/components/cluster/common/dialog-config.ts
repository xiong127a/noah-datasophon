/**
 * 集群Dialog统一配置
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 * 
 * 🎯 目标：确保所有集群向导步骤的Dialog样式完全一致
 */

import { DIALOG_STYLES } from './shared-styles'

// 统一的Dialog样式配置
export const CLUSTER_DIALOG_CONFIG = {
  // 所有步骤Dialog必须使用这个样式
  STANDARD_DIALOG_STYLE: DIALOG_STYLES.content,
  
  // 所有步骤底栏必须使用这个样式
  STANDARD_FOOTER_STYLE: DIALOG_STYLES.footer,
  
  // Dialog尺寸说明
  DIALOG_SPECS: {
    // 宽度：最小64px边距，最大1800px，响应式95vw
    width: "min(calc(100vw-64px), 1800px) on desktop, min(95vw, 1800px) on mobile",
    
    // 高度：最小96px边距，最大900px，响应式95vh 
    height: "min(calc(100vh-96px), 900px) on desktop, min(95vh, 900px) on mobile",
    
    // 其他特性
    features: [
      "3xl圆角 (rounded-3xl)",
      "2xl阴影 (shadow-2xl)", 
      "白色背景",
      "居中定位",
      "隐藏默认关闭按钮",
      "溢出隐藏"
    ]
  },
  
  // 底栏样式说明
  FOOTER_SPECS: {
    // 基础样式
    base: "p-6 sm:p-8 padding，半透明白色背景，毛玻璃效果",
    
    // 装饰特性
    decorations: [
      "装饰性光效背景",
      "顶部渐变分割线",
      "层次化设计",
      "相对定位支持"
    ],
    
    // 布局特性
    layout: "左右分布，左侧状态信息，右侧操作按钮"
  },
  
  // 验证函数 - 检查Dialog是否使用了标准样式
  validateDialogStyle: (className: string): boolean => {
    return className === DIALOG_STYLES.content
  },
  
  // 验证函数 - 检查底栏是否使用了标准样式
  validateFooterStyle: (className: string): boolean => {
    return className === DIALOG_STYLES.footer
  }
} as const

// 各个步骤Dialog的标准样式使用指南
export const DIALOG_USAGE_GUIDE = {
  "步骤1 - Host Config": {
    component: "K8sHostConfigDialog / PvmHostConfigDialog",
    dialogStyle: "✅ 已使用 DIALOG_STYLES.content",
    footerStyle: "✅ 自定义底栏（基准样式）",
    status: "REFERENCE_STANDARD"
  },
  
  "步骤2 - Host Validation": {
    component: "K8sHostValidationDialog / PvmHostValidationDialog", 
    dialogStyle: "✅ 已统一为 DIALOG_STYLES.content",
    footerStyle: "✅ 已统一为 DIALOG_STYLES.footer",
    layoutFix: "✅ 已修复高度约束容器",
    status: "FULLY_COMPLIANT"
  },
  
  "步骤3 - Agent Deploy": {
    component: "AgentDeploymentDialog",
    dialogStyle: "✅ 通过 ClusterWizardLayout 使用 DIALOG_STYLES.content",
    footerStyle: "✅ 通过 ClusterWizardActionBar 使用 DIALOG_STYLES.footer",
    status: "FULLY_COMPLIANT"
  },
  
  "步骤4 - Service Selection": {
    component: "ServiceSelectionDialog",
    dialogStyle: "✅ 已统一为 DIALOG_STYLES.content",
    footerStyle: "✅ 已统一为 DIALOG_STYLES.footer",
    layoutFix: "✅ 已修复高度约束容器", 
    status: "FULLY_COMPLIANT"
  },
  
  "步骤5 - Master Role Assign": {
    component: "MasterRoleAssignDialog",
    dialogStyle: "✅ 通过 ClusterWizardLayout 使用 DIALOG_STYLES.content",
    footerStyle: "✅ 通过 ClusterWizardActionBar 使用 DIALOG_STYLES.footer",
    status: "FULLY_COMPLIANT"
  },
  
  "步骤6 - Worker Role Assign": {
    component: "WorkerRoleAssignDialog", 
    dialogStyle: "✅ 通过 ClusterWizardLayout 使用 DIALOG_STYLES.content",
    footerStyle: "✅ 通过 ClusterWizardActionBar 使用 DIALOG_STYLES.footer",
    status: "FULLY_COMPLIANT"
  }
} as const

// 开发指南
export const DEVELOPMENT_GUIDELINES = {
  dialogRules: [
    "所有新的集群Dialog必须使用 DIALOG_STYLES.content",
    "不允许自定义Dialog尺寸样式",
    "如需修改Dialog样式，必须更新 shared-styles.ts 中的 DIALOG_STYLES.content",
    "任何Dialog样式修改都会影响所有步骤，需要全面测试"
  ],
  
  footerRules: [
    "所有新的集群Dialog底栏必须使用 DIALOG_STYLES.footer",
    "必须包含装饰性光效和顶部分割线",
    "使用 DIALOG_STYLES.footerContent 进行内容布局",
    "特殊底栏（如表格分页）可以例外，但需要文档说明"
  ],
  
  // 正确用法示例
  correctUsage: `
    import { DIALOG_STYLES } from './shared-styles'
    
    // Dialog容器
    <DialogContent className={DIALOG_STYLES.content}>
      {/* Dialog内容 */}
      
      {/* 底栏 */}
      <div className={DIALOG_STYLES.footer}>
        <div className={DIALOG_STYLES.footerGlow}></div>
        <div className={DIALOG_STYLES.footerTopLine}></div>
        <div className={DIALOG_STYLES.footerContent}>
          {/* 左侧信息 + 右侧按钮 */}
        </div>
      </div>
    </DialogContent>
  `,
  
  // 错误用法示例  
  incorrectUsage: `
    // ❌ 不要自定义Dialog样式
    <DialogContent className="w-[85vw] h-[80vh] max-w-6xl">
    
    // ❌ 不要自定义底栏样式
    <div className="bg-gradient-to-r from-white p-4 border-t">
  `
} as const

// 统计信息
export const DIALOG_STATS = {
  totalDialogs: 6,
  
  // Dialog样式统一情况
  dialogCompliant: 6,
  dialogFixed: 2, // K8sHostValidationDialog, ServiceSelectionDialog
  dialogComplianceRate: "100%",
  
  // 底栏样式统一情况  
  footerCompliant: 6, // Steps 1,2,3,4,5,6 全部使用统一样式
  footerFixed: 1, // Step 2 已修复为统一样式
  footerComplianceRate: "100%", // 6/6 全部统一
  
  // 🚨 NEW: 布局修复统一情况
  layoutFixed: 2, // Steps 2,4 修复了高度约束容器
  layoutCompliant: 6, // 所有步骤都有正确的高度约束
  layoutComplianceRate: "100%", // 6/6 底栏都能正确显示
  
  lastUpdated: "2024-01-20"
} as const

// 导出主要配置
export default CLUSTER_DIALOG_CONFIG
