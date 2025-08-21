"use client"

/**
 * Markdown渲染器主组件 - 苹果风格设计
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import remarkToc from 'remark-toc'

import rehypeSlug from 'rehype-slug'
import rehypeAutolinkHeadings from 'rehype-autolink-headings'

import { 
  AlertCircle, 
  BookOpen, 
  Download, 
  Search, 
  Settings, 
  Eye,
  FileText 
} from 'lucide-react'

import { docService } from '@/lib/api/doc-service'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { imageCache } from '@/lib/image-cache'
import { useCluster } from '@/hooks/useCluster'
import TableOfContents from './table-of-contents'
import ReadingProgress from './reading-progress'
import { 
  MarkdownViewerProps, 
  MarkdownViewerState, 
  TableOfContentsItem,
  ImageProps,
  LinkProps
} from './types'

// 导入样式
import './markdown-styles.css'
import './enhanced-styles.css'

const MarkdownViewer: React.FC<MarkdownViewerProps> = ({
  serviceId,
  serviceName,
  docType,
  clusterId: propClusterId
}) => {
  const { currentCluster } = useCluster()
  const effectiveClusterId = propClusterId || currentCluster?.id
  
  const [state, setState] = useState<MarkdownViewerState>({
    content: '',
    loading: true,
    error: null,
    hasContent: false,
    toc: [],
    activeHeading: null
  })

  const contentRef = useRef<HTMLDivElement>(null)
  const [showToc, setShowToc] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')

  // 获取文档内容
  const fetchDocument = useCallback(async () => {
    if (!effectiveClusterId || !serviceId) {
      setState(prev => ({ 
        ...prev, 
        loading: false, 
        error: '缺少必要参数：集群ID或服务ID' 
      }))
      return
    }

    setState(prev => ({ ...prev, loading: true, error: null }))

    try {
      const docData = await docService.getServiceDoc({
        clusterId: effectiveClusterId,
        serviceId,
        type: docType
      })

      if (!docData || !docData.hasContent || !docData.docContent) {
        setState(prev => ({
          ...prev,
          loading: false,
          hasContent: false,
          content: '',
          error: null
        }))
        return
      }

      // 处理markdown中的图片路径
      // 直接使用原始内容，让ImageComponent处理图片路径
      setState(prev => ({
        ...prev,
        content: docData.docContent, // 不预处理图片路径
        hasContent: true,
        loading: false,
        error: null
      }))
      
    } catch (error) {
      console.error('获取文档失败:', error)
      setState(prev => ({
        ...prev,
        loading: false,
        error: error instanceof Error ? error.message : '获取文档失败'
      }))
    }
  }, [effectiveClusterId, serviceId, docType])

  // 生成目录
  const generateTableOfContents = useCallback(() => {
    if (!contentRef.current) return

    const headings = contentRef.current.querySelectorAll('h1, h2, h3, h4, h5, h6')
    const tocItems: TableOfContentsItem[] = []
    const stack: { level: number; item: TableOfContentsItem }[] = []

    headings.forEach((heading) => {
      const level = parseInt(heading.tagName.charAt(1))
      const id = heading.id || heading.textContent?.toLowerCase()
        .replace(/\s+/g, '-')
        .replace(/[^\w\-]/g, '') || ''
      
      // 确保heading有id
      if (!heading.id && id) {
        heading.id = id
      }

      const item: TableOfContentsItem = {
        id,
        title: heading.textContent || '',
        level,
        children: []
      }

      // 构建层级结构
      while (stack.length > 0 && stack[stack.length - 1].level >= level) {
        stack.pop()
      }

      if (stack.length === 0) {
        tocItems.push(item)
      } else {
        const parent = stack[stack.length - 1].item
        if (!parent.children) parent.children = []
        parent.children.push(item)
      }

      stack.push({ level, item })
    })

    setState(prev => ({ ...prev, toc: tocItems }))
  }, [])

  // 在内容加载完成后生成目录
  useEffect(() => {
    if (state.hasContent && state.content && !state.loading) {
      // 延迟生成目录，确保DOM已渲染
      const timer = setTimeout(() => {
        generateTableOfContents()
      }, 500)
      return () => clearTimeout(timer)
    }
  }, [state.hasContent, state.content, state.loading, generateTableOfContents])

  // 滚动到指定标题
  const scrollToHeading = useCallback((id: string) => {
    const element = document.getElementById(id)
    if (element) {
      const yOffset = -80 // 偏移量，避免被固定头部遮挡
      const y = element.getBoundingClientRect().top + window.pageYOffset + yOffset
      
      window.scrollTo({
        top: y,
        behavior: 'smooth'
      })
      
      setState(prev => ({ ...prev, activeHeading: id }))
    }
  }, [])

  // 监听滚动，更新活跃标题
  useEffect(() => {
    const handleScroll = () => {
      if (!contentRef.current) return

      const headings = contentRef.current.querySelectorAll('h1, h2, h3, h4, h5, h6')
      let current = ''

      headings.forEach((heading) => {
        const rect = heading.getBoundingClientRect()
        if (rect.top <= 100) {
          current = heading.id
        }
      })

      if (current !== state.activeHeading) {
        setState(prev => ({ ...prev, activeHeading: current }))
      }
    }

    const throttledHandleScroll = () => {
      requestAnimationFrame(handleScroll)
    }

    window.addEventListener('scroll', throttledHandleScroll)
    return () => window.removeEventListener('scroll', throttledHandleScroll)
  }, [state.activeHeading])

  // 初始化数据获取
  useEffect(() => {
    fetchDocument()
  }, [fetchDocument])

  // 组件卸载时清理图片缓存
  useEffect(() => {
    return () => {
      imageCache.clearExpiredCache(0) // 立即清理所有缓存
    }
  }, [])







  // 自定义图片渲染 - 使用缓存机制防止重复请求
  const ImageComponent: React.FC<ImageProps> = ({ src, alt, title }) => {
    const [imageState, setImageState] = useState(() => {
      // 初始化时检查缓存
      if (!src) return { loading: false, error: true, url: '' }
      
      const status = imageCache.getImageStatus(src)
      return {
        loading: status.loading,
        error: status.error,
        url: status.url || ''
      }
    })

    // 使用useMemo确保src稳定
    const stableSrc = useMemo(() => src, [src])

    // 异步加载图片 - 使用缓存
    useEffect(() => {
      if (!stableSrc) {
        setImageState({ loading: false, error: true, url: '' })
        return
      }

      // 检查缓存状态
      const status = imageCache.getImageStatus(stableSrc)
      if (status.url && !status.error) {
        // 缓存命中
        setImageState({ loading: false, error: false, url: status.url })
        return
      }

      if (status.loading) {
        // 正在加载中
        setImageState({ loading: true, error: false, url: '' })
        return
      }

      // 开始加载
      const loadImage = async () => {
        try {
          setImageState({ loading: true, error: false, url: '' })
          
          const imageUrl = await imageCache.getImageUrl(stableSrc, (path) => 
            clusterApiV1.doc.getImageBlob(path)
          )
          
          setImageState({ loading: false, error: false, url: imageUrl })
          
        } catch {
          setImageState({ loading: false, error: true, url: '' })
        }
      }

      loadImage()
    }, [stableSrc])

    const handleImageError = () => {
      setImageState(prev => ({ ...prev, error: true, loading: false }))
    }

    const handleImageLoad = () => {
      setImageState(prev => ({ ...prev, loading: false }))
    }

    if (imageState.error) {
      // 使用span避免p标签嵌套问题
      return (
        <span className="image-error block w-full text-center p-8 bg-red-50 border border-red-200 rounded-lg">
          <AlertCircle className="w-8 h-8 text-gray-400 mx-auto mb-2" />
          <span className="text-sm text-gray-500 block">图片加载失败</span>
          <span className="text-xs text-gray-400 block mt-1">路径: {src}</span>
          {alt && <span className="text-xs text-gray-400 block mt-1">描述: {alt}</span>}
        </span>
      )
    }

    return (
      <>
        {imageState.loading && (
          <span className="image-loading block w-full text-center p-8 bg-gray-50 border border-gray-200 rounded-lg mb-4">
            <span className="loading-spinner inline-block w-5 h-5 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin" />
            <span className="text-sm text-gray-500 ml-2">加载中... {src}</span>
          </span>
        )}
        {imageState.url && (
          <img
            src={imageState.url}
            alt={alt}
            title={title}
            onError={handleImageError}
            onLoad={handleImageLoad}
            className="markdown-image block w-full max-w-full h-auto my-4 rounded-lg shadow-lg bg-white p-2"
            style={{ display: imageState.loading ? 'none' : 'block' }}
          />
        )}
      </>
    )
  }

  // 自定义链接渲染
  const LinkComponent: React.FC<LinkProps> = ({ href, children }) => {
    const isExternal = href?.startsWith('http')
    
    return (
      <a
        href={href}
        target={isExternal ? '_blank' : undefined}
        rel={isExternal ? 'noopener noreferrer' : undefined}
        className="markdown-link"
      >
        {children}
      </a>
    )
  }

  // 渲染加载状态
  if (state.loading) {
    return (
      <div className="markdown-viewer">
        <div className="markdown-loading">
          <div className="loading-spinner" />
          <h3 className="text-lg font-medium text-gray-700 mb-2">正在加载文档</h3>
          <p className="text-gray-500">
            正在获取 {serviceName} 的{docType === 'component' ? '组件介绍' : '用户指南'}
          </p>
        </div>
      </div>
    )
  }

  // 渲染错误状态
  if (state.error) {
    return (
      <div className="markdown-viewer">
        <div className="markdown-error">
          <AlertCircle className="w-12 h-12 mb-4" />
          <h3 className="text-lg font-medium mb-2">加载失败</h3>
          <p className="text-sm mb-4">{state.error}</p>
          <button
            onClick={fetchDocument}
            className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
          >
            重试
          </button>
        </div>
      </div>
    )
  }

  // 渲染空状态
  if (!state.hasContent) {
    const docTypeName = docType === 'component' ? '组件介绍' : '用户指南'
    
    return (
      <div className="markdown-viewer">
        <div className="markdown-empty">
          <FileText className="w-16 h-16 mb-4 text-gray-300" />
          <h3 className="text-lg font-medium text-gray-700 mb-2">暂无{docTypeName}</h3>
          <p className="text-gray-500 text-center max-w-md">
            {serviceName} 服务的{docTypeName}文档暂未提供，
            <br />
            请联系管理员或查看其他相关文档。
          </p>
        </div>
      </div>
    )
  }

  // 渲染主要内容
  return (
    <>
              <ReadingProgress targetRef={contentRef as React.RefObject<HTMLElement>} />
      
      <div className="markdown-viewer">
        {/* 工具栏 */}
        <div className="markdown-toolbar">
          <div className="flex items-center justify-between p-4 border-b border-gray-200 bg-gradient-to-r from-white via-blue-50/30 to-indigo-50/20 backdrop-blur-sm">
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 shadow-lg">
                  <BookOpen className="w-5 h-5 text-white" />
                </div>
                <div>
                  <h2 className="font-bold text-gray-900 text-lg">
                    {serviceName}
                  </h2>
                  <p className="text-sm text-blue-600 font-medium">
                    {docType === 'component' ? '🔧 组件介绍' : '📖 用户指南'}
                  </p>
                </div>
              </div>
              
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => setShowToc(!showToc)}
                  className={`p-2.5 rounded-xl transition-all duration-200 shadow-sm border ${
                    showToc 
                      ? 'bg-gradient-to-r from-blue-500 to-indigo-600 text-white border-blue-400 shadow-blue-100' 
                      : 'text-gray-600 hover:bg-white hover:text-blue-600 border-gray-200 hover:border-blue-300 hover:shadow-md'
                  }`}
                  title={showToc ? '隐藏目录' : '显示目录'}
                >
                  <Eye className="w-4 h-4" />
                </button>
                
                <button
                  className="p-2.5 text-gray-600 hover:bg-white hover:text-indigo-600 rounded-xl transition-all duration-200 shadow-sm border border-gray-200 hover:border-indigo-300 hover:shadow-md"
                  title="设置"
                >
                  <Settings className="w-4 h-4" />
                </button>
                
                <button
                  className="p-2.5 text-gray-600 hover:bg-white hover:text-green-600 rounded-xl transition-all duration-200 shadow-sm border border-gray-200 hover:border-green-300 hover:shadow-md"
                  title="下载PDF"
                >
                  <Download className="w-4 h-4" />
                </button>
              </div>
            </div>
            
            {/* 搜索框 */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="🔍 搜索文档内容..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-300 bg-white/80 backdrop-blur-sm shadow-sm hover:shadow-md transition-all duration-200 placeholder:text-gray-400"
              />
            </div>
          </div>
        </div>

        {/* 侧边栏目录 - 移到外层确保fixed定位生效 */}
        {showToc && (
          <div className="markdown-sidebar">
            <TableOfContents
              items={state.toc}
              activeId={state.activeHeading || undefined}
              onItemClick={scrollToHeading}
            />
          </div>
        )}

        {/* 主要内容区域 */}
        <div className="markdown-layout">
          {/* 文档内容 */}
          <div 
            ref={contentRef}
            className={`markdown-content ${!showToc ? 'full-width' : ''}`}
          >
            <ReactMarkdown
              remarkPlugins={[remarkGfm, remarkToc]}
              rehypePlugins={[
                rehypeSlug,
                [rehypeAutolinkHeadings, { behavior: 'wrap' }]
              ]}
              components={{
                // 禁用代码块，让所有代码内容作为普通文本显示
                code: ({ children }) => <span className="inline-text">{children}</span>,
                pre: ({ children }) => <div className="text-content">{children}</div>,
                img: ImageComponent,
                a: LinkComponent,
              }}
              // 跳过HTML处理，避免 < > 符号被误识别为HTML标签
              skipHtml={true}
            >
              {state.content}
            </ReactMarkdown>
          </div>
        </div>
      </div>
    </>
  )
}

export default MarkdownViewer
