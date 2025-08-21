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
  Eye
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
  const [isMobile, setIsMobile] = useState(false)
  const [searchResults, setSearchResults] = useState<{ index: number; total: number }>({ index: 0, total: 0 })
  const [isSearching, setIsSearching] = useState(false)
  const [isDownloading, setIsDownloading] = useState(false)
  const [showSettings, setShowSettings] = useState(false)
  const [settings, setSettings] = useState({
    fontSize: 'medium', // small, medium, large
    theme: 'light', // light, dark
    readingWidth: 'normal' // narrow, normal, wide
  })

  // 检测移动端
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth <= 768)
    }
    
    checkMobile()
    window.addEventListener('resize', checkMobile)
    return () => window.removeEventListener('resize', checkMobile)
  }, [])

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

  // 搜索功能实现
  const performSearch = useCallback((term: string) => {
    if (!term.trim() || !contentRef.current) {
      // 清除之前的高亮
      const highlighted = contentRef.current?.querySelectorAll('.search-highlight')
      highlighted?.forEach(el => {
        const parent = el.parentNode
        if (parent) {
          parent.replaceChild(document.createTextNode(el.textContent || ''), el)
          parent.normalize()
        }
      })
      setSearchResults({ index: 0, total: 0 })
      setIsSearching(false)
      return
    }

    setIsSearching(true)
    
    // 清除之前的高亮
    const highlighted = contentRef.current.querySelectorAll('.search-highlight')
    highlighted.forEach(el => {
      const parent = el.parentNode
      if (parent) {
        parent.replaceChild(document.createTextNode(el.textContent || ''), el)
        parent.normalize()
      }
    })

    // 查找并高亮所有匹配项
    const walker = document.createTreeWalker(
      contentRef.current,
      NodeFilter.SHOW_TEXT,
      null
    )

    const textNodes: Text[] = []
    let node: Node | null
    while ((node = walker.nextNode())) {
      textNodes.push(node as Text)
    }

    let matchCount = 0
    const regex = new RegExp(term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi')

    textNodes.forEach(textNode => {
      const text = textNode.textContent || ''
      if (regex.test(text)) {
        const parent = textNode.parentNode
        if (parent) {
          const highlightedHTML = text.replace(regex, (match) => {
            matchCount++
            return `<mark class="search-highlight" data-search-index="${matchCount - 1}">${match}</mark>`
          })
          
          const wrapper = document.createElement('span')
          wrapper.innerHTML = highlightedHTML
          parent.replaceChild(wrapper, textNode)
        }
      }
    })

    setSearchResults({ index: matchCount > 0 ? 1 : 0, total: matchCount })
    
    // 滚动到第一个匹配项
    if (matchCount > 0) {
      setTimeout(() => {
        const firstMatch = contentRef.current?.querySelector('.search-highlight[data-search-index="0"]')
        if (firstMatch) {
          firstMatch.scrollIntoView({ behavior: 'smooth', block: 'center' })
          firstMatch.classList.add('current-match')
        }
      }, 100)
    }
    
    setIsSearching(false)
  }, [])

  // 搜索导航
  const navigateSearch = useCallback((direction: 'next' | 'prev') => {
    if (searchResults.total === 0) return

    const currentHighlight = contentRef.current?.querySelector('.current-match')
    currentHighlight?.classList.remove('current-match')

    let newIndex: number
    if (direction === 'next') {
      newIndex = searchResults.index >= searchResults.total ? 1 : searchResults.index + 1
    } else {
      newIndex = searchResults.index <= 1 ? searchResults.total : searchResults.index - 1
    }

    const targetMatch = contentRef.current?.querySelector(`.search-highlight[data-search-index="${newIndex - 1}"]`)
    if (targetMatch) {
      targetMatch.scrollIntoView({ behavior: 'smooth', block: 'center' })
      targetMatch.classList.add('current-match')
      setSearchResults({ ...searchResults, index: newIndex })
    }
  }, [searchResults])

  // 搜索防抖
  useEffect(() => {
    const timer = setTimeout(() => {
      performSearch(searchTerm)
    }, 300)

    return () => clearTimeout(timer)
  }, [searchTerm, performSearch])

  // PDF下载功能 - 使用jsPDF和html2canvas直接生成PDF文件
  const handleDownloadPDF = useCallback(async () => {
    if (!state.hasContent || !contentRef.current) return

    setIsDownloading(true)
    
    try {
      // 动态导入库
      const [{ default: jsPDF }, { default: html2canvas }] = await Promise.all([
        import('jspdf'),
        import('html2canvas')
      ])

      // 创建用于截图的容器
      const printContainer = document.createElement('div')
      printContainer.style.cssText = `
        position: absolute;
        top: -9999px;
        left: -9999px;
        width: 794px;
        padding: 40px;
        background: white;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        font-size: 14px;
        line-height: 1.6;
        color: #000;
      `

      // 克隆内容并清理搜索高亮
      const content = contentRef.current.cloneNode(true) as HTMLElement
      const highlights = content.querySelectorAll('.search-highlight')
      highlights.forEach(highlight => {
        const textNode = document.createTextNode(highlight.textContent || '')
        highlight.parentNode?.replaceChild(textNode, highlight)
      })

      // 添加标题和内容
      printContainer.innerHTML = `
        <h1 style="font-size: 24px; margin: 0 0 20px 0; color: #333; border-bottom: 2px solid #007aff; padding-bottom: 10px;">
          ${serviceName} - ${docType === 'component' ? '组件介绍' : '用户指南'}
        </h1>
        ${content.innerHTML}
      `

      // 添加到DOM中
      document.body.appendChild(printContainer)

      // 等待图片加载
      const images = printContainer.querySelectorAll('img')
      await Promise.all(Array.from(images).map(img => {
        return new Promise((resolve) => {
          if (img.complete) {
            resolve(null)
          } else {
            img.onload = () => resolve(null)
            img.onerror = () => resolve(null)
            // 5秒超时
            setTimeout(() => resolve(null), 5000)
          }
        })
      }))

      // 生成截图
      const canvas = await html2canvas(printContainer, {
        scale: 2,
        useCORS: true,
        allowTaint: false,
        backgroundColor: '#ffffff',
        logging: false
      })

      // 移除临时容器
      document.body.removeChild(printContainer)

      // 创建PDF
      const pdf = new jsPDF('p', 'mm', 'a4')
      const imgData = canvas.toDataURL('image/jpeg', 0.8)
      
      // A4纸张尺寸（毫米）
      const pdfWidth = 210
      const pdfHeight = 297
      const margin = 15
      const maxWidth = pdfWidth - (margin * 2)
      const maxHeight = pdfHeight - (margin * 2)

      // 计算图片尺寸
      const imgWidth = canvas.width
      const imgHeight = canvas.height
      const ratio = Math.min(maxWidth / (imgWidth * 0.264583), maxHeight / (imgHeight * 0.264583))
      
      const finalWidth = (imgWidth * 0.264583) * ratio
      const finalHeight = (imgHeight * 0.264583) * ratio

      // 如果内容超过一页，分页处理
      if (finalHeight > maxHeight) {
        let position = 0
        let page = 1
        
        while (position < finalHeight) {
          if (page > 1) {
            pdf.addPage()
          }
          
          const remainingHeight = finalHeight - position
          const pageHeight = Math.min(maxHeight, remainingHeight)
          
          // 计算源图片中对应的区域
          const sourceY = (position / finalHeight) * imgHeight
          const sourceHeight = (pageHeight / finalHeight) * imgHeight
          
          // 创建当前页的canvas
          const pageCanvas = document.createElement('canvas')
          pageCanvas.width = imgWidth
          pageCanvas.height = sourceHeight
          const pageCtx = pageCanvas.getContext('2d')
          
          if (pageCtx) {
            pageCtx.drawImage(canvas, 0, sourceY, imgWidth, sourceHeight, 0, 0, imgWidth, sourceHeight)
            const pageImgData = pageCanvas.toDataURL('image/jpeg', 0.8)
            pdf.addImage(pageImgData, 'JPEG', margin, margin, finalWidth, pageHeight)
          }
          
          position += pageHeight
          page++
        }
      } else {
        // 单页内容
        pdf.addImage(imgData, 'JPEG', margin, margin, finalWidth, finalHeight)
      }

      // 下载PDF
      const fileName = `${serviceName}-${docType === 'component' ? '组件介绍' : '用户指南'}.pdf`
      pdf.save(fileName)
      
    } catch (error) {
      console.error('PDF生成失败:', error)
      alert('PDF生成失败，请稍后重试')
    } finally {
      setIsDownloading(false)
    }
  }, [state.hasContent, serviceName, docType])

  // 点击外部关闭设置菜单
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (showSettings && !(event.target as Element).closest('.settings-menu')) {
        setShowSettings(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [showSettings])

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
        <div className="flex items-center justify-center min-h-[500px]">
          <div className="flex flex-col items-center space-y-6 text-center">
            <div className="relative">
              <div className="w-16 h-16 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin"></div>
              <div className="absolute inset-0 w-16 h-16 border-4 border-transparent border-b-blue-400 rounded-full animate-pulse"></div>
            </div>
            <div>
              <h3 className="text-xl font-bold text-gray-800 mb-2">正在加载文档</h3>
              <p className="text-gray-600 mb-4">
                正在获取 <span className="font-medium text-blue-600">{serviceName}</span> 的
                <span className="font-medium">{docType === 'component' ? '组件介绍' : '用户指南'}</span>
              </p>
              <p className="text-sm text-gray-500">请稍候片刻...</p>
            </div>
            <div className="w-64 h-3 bg-gray-200 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-blue-500 via-indigo-600 to-purple-600 rounded-full animate-pulse"></div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // 渲染错误状态
  if (state.error) {
    return (
      <div className="markdown-viewer">
        <div className="flex items-center justify-center min-h-[500px]">
          <div className="text-center max-w-lg mx-auto p-8">
            <div className="w-20 h-20 mx-auto mb-6 text-red-500">
              <AlertCircle className="w-full h-full" />
            </div>
            <h3 className="text-2xl font-bold text-gray-800 mb-3">文档加载失败</h3>
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
              <p className="text-red-800 font-medium mb-2">错误详情：</p>
              <p className="text-red-700 text-sm leading-relaxed">{state.error}</p>
            </div>
            <p className="text-gray-600 mb-6">
              请检查网络连接状态，或稍后重试加载文档
            </p>
            <div className="flex justify-center space-x-4">
              <button 
                onClick={fetchDocument}
                className="px-6 py-3 bg-gradient-to-r from-red-600 to-red-700 text-white rounded-xl hover:from-red-700 hover:to-red-800 transition-all duration-200 shadow-md hover:shadow-lg transform hover:scale-105 flex items-center space-x-2"
              >
                <span>🔄</span>
                <span>重新加载</span>
              </button>
              <button 
                onClick={() => window.location.reload()}
                className="px-6 py-3 bg-gray-200 text-gray-700 rounded-xl hover:bg-gray-300 transition-all duration-200 flex items-center space-x-2"
              >
                <span>↻</span>
                <span>刷新页面</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // 渲染空状态
  if (!state.hasContent) {
    const docTypeName = docType === 'component' ? '组件介绍' : '用户指南'
    
    return (
      <div className="markdown-viewer">
        <div className="flex items-center justify-center min-h-[500px]">
          <div className="text-center max-w-lg mx-auto p-8">
            <div className="w-24 h-24 mx-auto mb-6 text-gray-400">
              <BookOpen className="w-full h-full" />
            </div>
            <h3 className="text-2xl font-bold text-gray-800 mb-3">暂无{docTypeName}</h3>
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 mb-6">
              <p className="text-gray-700 leading-relaxed">
                <span className="font-medium text-blue-600">{serviceName}</span> 服务的{docTypeName}文档暂未提供
              </p>
            </div>
            <div className="space-y-2 text-gray-600">
              <p className="flex items-center justify-center space-x-2">
                <span>📝</span>
                <span>文档可能正在编写中</span>
              </p>
              <p className="flex items-center justify-center space-x-2">
                <span>🔄</span>
                <span>请稍后查看或联系管理员</span>
              </p>
            </div>
            <button 
              onClick={fetchDocument}
              className="mt-6 px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-xl hover:from-blue-700 hover:to-indigo-700 transition-all duration-200 shadow-md hover:shadow-lg transform hover:scale-105"
            >
              重新检查
            </button>
          </div>
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
                
                <div className="relative settings-menu">
                  <button
                    onClick={() => setShowSettings(!showSettings)}
                    className={`p-2.5 rounded-xl transition-all duration-200 shadow-sm border ${
                      showSettings
                        ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white border-indigo-400 shadow-indigo-100'
                        : 'text-gray-600 hover:bg-white hover:text-indigo-600 border-gray-200 hover:border-indigo-300 hover:shadow-md'
                    }`}
                    title="设置"
                  >
                    <Settings className="w-4 h-4" />
                  </button>
                  
                  {/* 设置菜单 */}
                  {showSettings && (
                    <div className="absolute right-0 top-full mt-2 w-64 bg-white rounded-xl shadow-lg border border-gray-200 py-3 z-50">
                      <div className="px-4 pb-2 border-b border-gray-100">
                        <h3 className="text-sm font-semibold text-gray-800">阅读设置</h3>
                      </div>
                      
                      {/* 字体大小 */}
                      <div className="px-4 py-3 border-b border-gray-100">
                        <label className="block text-xs font-medium text-gray-600 mb-2">字体大小</label>
                        <div className="flex space-x-2">
                          {[
                            { value: 'small', label: '小' },
                            { value: 'medium', label: '中' },
                            { value: 'large', label: '大' }
                          ].map((size) => (
                            <button
                              key={size.value}
                              onClick={() => setSettings(prev => ({ ...prev, fontSize: size.value }))}
                              className={`px-3 py-1.5 text-xs rounded-lg transition-colors ${
                                settings.fontSize === size.value
                                  ? 'bg-blue-100 text-blue-700 border border-blue-200'
                                  : 'bg-gray-50 text-gray-600 hover:bg-gray-100'
                              }`}
                            >
                              {size.label}
                            </button>
                          ))}
                        </div>
                      </div>
                      
                      {/* 主题模式 */}
                      <div className="px-4 py-3 border-b border-gray-100">
                        <label className="block text-xs font-medium text-gray-600 mb-2">主题模式</label>
                        <div className="flex space-x-2">
                          {[
                            { value: 'light', label: '浅色', icon: '☀️' },
                            { value: 'dark', label: '深色', icon: '🌙' }
                          ].map((theme) => (
                            <button
                              key={theme.value}
                              onClick={() => setSettings(prev => ({ ...prev, theme: theme.value }))}
                              className={`flex items-center space-x-1 px-3 py-1.5 text-xs rounded-lg transition-colors ${
                                settings.theme === theme.value
                                  ? 'bg-blue-100 text-blue-700 border border-blue-200'
                                  : 'bg-gray-50 text-gray-600 hover:bg-gray-100'
                              }`}
                            >
                              <span>{theme.icon}</span>
                              <span>{theme.label}</span>
                            </button>
                          ))}
                        </div>
                      </div>
                      
                      {/* 阅读宽度 */}
                      <div className="px-4 py-3">
                        <label className="block text-xs font-medium text-gray-600 mb-2">阅读宽度</label>
                        <div className="flex space-x-2">
                          {[
                            { value: 'narrow', label: '窄' },
                            { value: 'normal', label: '普通' },
                            { value: 'wide', label: '宽' }
                          ].map((width) => (
                            <button
                              key={width.value}
                              onClick={() => setSettings(prev => ({ ...prev, readingWidth: width.value }))}
                              className={`px-3 py-1.5 text-xs rounded-lg transition-colors ${
                                settings.readingWidth === width.value
                                  ? 'bg-blue-100 text-blue-700 border border-blue-200'
                                  : 'bg-gray-50 text-gray-600 hover:bg-gray-100'
                              }`}
                            >
                              {width.label}
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
                
                <button
                  onClick={handleDownloadPDF}
                  disabled={isDownloading || !state.hasContent}
                  className={`p-2.5 rounded-xl transition-all duration-200 shadow-sm border ${
                    isDownloading 
                      ? 'bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed'
                      : state.hasContent
                        ? 'text-gray-600 hover:bg-white hover:text-green-600 border-gray-200 hover:border-green-300 hover:shadow-md'
                        : 'text-gray-300 border-gray-100 cursor-not-allowed'
                  }`}
                  title={isDownloading ? '正在生成PDF文件...' : '下载为PDF文件'}
                >
                  {isDownloading ? (
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-green-500"></div>
                  ) : (
                    <Download className="w-4 h-4" />
                  )}
                </button>
              </div>
            </div>
            
            {/* 搜索框 */}
            <div className="relative flex items-center space-x-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="🔍 搜索文档内容..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-300 bg-white/80 backdrop-blur-sm shadow-sm hover:shadow-md transition-all duration-200 placeholder:text-gray-400"
                />
                {isSearching && (
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-500"></div>
                  </div>
                )}
              </div>
              
              {/* 搜索结果和导航 */}
              {searchTerm && searchResults.total > 0 && (
                <div className="flex items-center space-x-1">
                  <span className="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
                    {searchResults.index}/{searchResults.total}
                  </span>
                  <button
                    onClick={() => navigateSearch('prev')}
                    className="p-1 text-gray-500 hover:text-blue-600 transition-colors"
                    title="上一个"
                    disabled={searchResults.total === 0}
                  >
                    ▲
                  </button>
                  <button
                    onClick={() => navigateSearch('next')}
                    className="p-1 text-gray-500 hover:text-blue-600 transition-colors"
                    title="下一个"
                    disabled={searchResults.total === 0}
                  >
                    ▼
                  </button>
                </div>
              )}
              
              {searchTerm && searchResults.total === 0 && !isSearching && (
                <span className="text-xs text-red-500 bg-red-50 px-2 py-1 rounded">
                  无结果
                </span>
              )}
            </div>
          </div>
        </div>

        {/* 侧边栏目录 - 移到外层确保fixed定位生效 */}
        {showToc && (
          <>
            {/* 移动端遮罩层 */}
            {isMobile && (
              <div 
                className="fixed inset-0 bg-black/30 z-40"
                onClick={() => setShowToc(false)}
              />
            )}
            <div className={`markdown-sidebar ${isMobile && showToc ? 'mobile-show' : ''}`}>
              <TableOfContents
                items={state.toc}
                activeId={state.activeHeading || undefined}
                onItemClick={scrollToHeading}
              />
            </div>
          </>
        )}

        {/* 主要内容区域 */}
        <div className="markdown-layout">
          {/* 文档内容 */}
          <div 
            ref={contentRef}
            className={`markdown-content ${!showToc ? 'full-width' : ''} ${
              settings.fontSize === 'small' ? 'text-sm' : 
              settings.fontSize === 'large' ? 'text-lg' : 'text-base'
            } ${
              settings.theme === 'dark' ? 'dark-theme' : ''
            } ${
              settings.readingWidth === 'narrow' ? 'max-w-2xl' :
              settings.readingWidth === 'wide' ? 'max-w-none' : 'max-w-4xl'
            }`}
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
