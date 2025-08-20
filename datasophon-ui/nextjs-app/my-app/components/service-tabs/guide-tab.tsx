"use client"

/**
 * 用户指南页面 - 使用新的MarkdownViewer
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import MarkdownViewer from '@/components/markdown-viewer/markdown-viewer'

interface GuideTabProps {
  serviceId: string
  serviceName: string
}

export default function GuideTab({ serviceId, serviceName }: GuideTabProps) {
  return (
    <div className="h-full bg-gray-50">
      <MarkdownViewer
        serviceId={serviceId}
        serviceName={serviceName}
        docType="guide"
      />
    </div>
  )
}
