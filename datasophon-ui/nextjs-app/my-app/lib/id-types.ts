/**
 * ID类型定义 - 统一管理所有ID为string类型
 * 避免20位LONG在JavaScript中的精度丢失问题
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

// 基础ID类型 - 统一为string
export type ID = string

// 具体业务ID类型
export type ClusterId = string
export type ServiceId = string
export type HostId = string
export type UserId = string
export type RoleId = string
export type GroupId = string
export type TenantId = string
export type RackId = string
export type LabelId = string
export type ParcelId = string
export type AlertGroupId = string
export type NoticeGroupId = string
export type ConfigId = string
export type CommandId = string

// ID验证工具
export class IDValidator {
  /**
   * 检查是否为有效的ID格式
   * @param id 要检查的ID
   * @returns 是否有效
   */
  static isValidId(id: unknown): id is string {
    return typeof id === 'string' && id.trim() !== '' && /^\d+$/.test(id)
  }

  /**
   * 确保ID为字符串格式
   * @param id 输入ID（可能是string或number）
   * @returns 字符串格式的ID
   */
  static ensureStringId(id: string | number | undefined | null): string {
    if (id === undefined || id === null) {
      throw new Error('ID不能为空')
    }
    
    if (typeof id === 'number') {
      // 检查是否超出JavaScript安全整数范围
      if (!Number.isSafeInteger(id)) {
        console.warn(`ID ${id} 超出JavaScript安全整数范围，可能存在精度丢失`)
      }
      return id.toString()
    }
    
    if (typeof id === 'string') {
      const trimmedId = id.trim()
      if (trimmedId === '') {
        throw new Error('ID不能为空字符串')
      }
      return trimmedId
    }
    
    throw new Error(`无效的ID类型: ${typeof id}`)
  }

  /**
   * 批量转换ID数组
   * @param ids ID数组
   * @returns 字符串格式的ID数组
   */
  static ensureStringIds(ids: (string | number)[]): string[] {
    return ids.map(id => this.ensureStringId(id))
  }

  /**
   * 检查ID是否可能是长整型（超过16位）
   * @param id 要检查的ID
   * @returns 是否可能是长整型
   */
  static isPotentialLongId(id: string): boolean {
    return /^\d{17,}$/.test(id) // 17位以上的数字
  }
}

// 常用的ID转换函数
export const toStringId = IDValidator.ensureStringId
export const toStringIds = IDValidator.ensureStringIds

// 类型守卫
export function isStringId(value: unknown): value is string {
  return IDValidator.isValidId(value)
}

// 默认导出
export default {
  IDValidator,
  toStringId,
  toStringIds,
  isStringId
}
