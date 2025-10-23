/**
 * 文档服务API封装 - 使用统一的API工具类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import { clusterApiV1 } from '@/lib/api-utils-v1'

export interface ServiceDocParams {
  clusterId: string
  serviceId: string
  type: 'component' | 'guide'
}

export interface ServiceDocVO {
  clusterId: string
  serviceId: string
  serviceName: string
  docType: string
  docTypeDisplayName: string
  docContent: string
  formattedContent?: string
  docPath?: string
  contentLength: number
  contentLengthText: string
  encoding: string
  hasContent: boolean
  lastModified?: string
}

export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

class DocServiceClass {
  /**
   * 获取服务文档内容
   */
  async getServiceDoc(params: ServiceDocParams): Promise<ServiceDocVO> {
    try {
      const response = await clusterApiV1.doc.getServiceDoc(params)
      
      if (response.data.code !== 200) {
        throw new Error(response.data.msg || '获取文档失败')
      }

      if (!response.data.data) {
        throw new Error('服务器返回数据为空')
      }

      return response.data.data
    } catch (error) {
      console.error('获取服务文档失败:', error)
      throw error
    }
  }

  /**
   * 检查服务文档是否存在
   */
  async hasServiceDoc(params: ServiceDocParams): Promise<boolean> {
    try {
      const response = await clusterApiV1.doc.hasServiceDoc(params)
      return response.data.code === 200 && response.data.data === true
    } catch (error) {
      console.error('检查文档存在性失败:', error)
      return false
    }
  }

  /**
   * 获取服务名称
   */
  async getServiceName(serviceId: string): Promise<string> {
    try {
      const response = await clusterApiV1.doc.getServiceName(serviceId)
      
      if (response.data.code !== 200) {
        throw new Error(response.data.msg || '获取服务名称失败')
      }

      return response.data.data || ''
    } catch (error) {
      console.error('获取服务名称失败:', error)
      throw error
    }
  }

  /**
   * 获取图片资源URL
   * 处理markdown中的图片路径，转换为可访问的API地址
   */
  getImageUrl(imagePath: string): string {
    return clusterApiV1.doc.getImageUrl(imagePath)
  }

  /**
   * 处理markdown内容中的图片路径（暂时不使用，由ImageComponent直接处理）
   */
  processMarkdownImages(content: string): string {
    // 暂时直接返回原内容，避免重复处理
    return content
    
    // 原处理逻辑保留备用
    // if (!content) return ''
    // return content.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (match, alt, src) => {
    //   const processedSrc = this.getImageUrl(src)
    //   return `![${alt}](${processedSrc})`
    // })
  }

  /**
   * 测试图片URL生成（生产环境禁用）
   */
  testImageUrl(): void {
    return // 生产环境下禁用调试功能
  }
}

// 导出单例实例
export const docService = new DocServiceClass()
