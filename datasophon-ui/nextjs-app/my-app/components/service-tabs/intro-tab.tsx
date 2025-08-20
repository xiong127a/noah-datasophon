"use client"

/**
 * 组件介绍页面 - 使用新的MarkdownViewer
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import MarkdownViewer from '@/components/markdown-viewer/markdown-viewer'

interface IntroTabProps {
  serviceId: string
  serviceName: string
}

export default function IntroTab({ serviceId, serviceName }: IntroTabProps) {
  return (
    <div className="h-full bg-gray-50">
      <MarkdownViewer
        serviceId={serviceId}
        serviceName={serviceName}
        docType="component"
      />
    </div>
  )
}
