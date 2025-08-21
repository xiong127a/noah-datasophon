/**
 * Markdown渲染组件类型定义
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

export interface MarkdownViewerProps {
  serviceId: string
  serviceName: string
  docType: 'component' | 'guide'
  clusterId?: string
  isFullScreen?: boolean  // 是否全屏模式
}

export interface TableOfContentsItem {
  id: string
  title: string
  level: number
  children?: TableOfContentsItem[]
}

export interface MarkdownViewerState {
  content: string
  loading: boolean
  error: string | null
  hasContent: boolean
  toc: TableOfContentsItem[]
  activeHeading: string | null
}

export interface ReadingProgressProps {
  targetRef: React.RefObject<HTMLElement>
}

export interface TableOfContentsProps {
  items: TableOfContentsItem[]
  activeId?: string
  onItemClick: (id: string) => void
}

export interface MarkdownContentProps {
  content: string
  onHeadingClick?: (id: string) => void
}

export interface CodeBlockProps {
  children: React.ReactNode
  className?: string
  inline?: boolean
}

export interface ImageProps {
  src?: string
  alt?: string
  title?: string
}

export interface LinkProps {
  href?: string
  children: React.ReactNode
  target?: string
  rel?: string
}

// 文档加载状态
export type LoadingState = 'idle' | 'loading' | 'success' | 'error'
