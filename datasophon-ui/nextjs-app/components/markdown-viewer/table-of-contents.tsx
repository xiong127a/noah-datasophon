"use client"

/**
 * 目录组件 - 苹果风格设计
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import React, { useState, useEffect } from 'react'
import { ChevronRight, ChevronDown, BookOpen, List } from 'lucide-react'
import { TableOfContentsProps, TableOfContentsItem } from './types'

const TOCItem: React.FC<{
  item: TableOfContentsItem
  activeId?: string
  onItemClick: (id: string) => void
  level: number
}> = ({ item, activeId, onItemClick, level }) => {
  const [isExpanded, setIsExpanded] = useState(true)
  const hasChildren = item.children && item.children.length > 0
  const isActive = activeId === item.id
  const isChildActive = item.children?.some(child => child.id === activeId)

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault()
    onItemClick(item.id)
  }

  const toggleExpanded = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsExpanded(!isExpanded)
  }

  const paddingLeft = `${0.75 + level * 0.75}rem`

  return (
    <div className="toc-item">
      <div 
        className={`
          group flex items-center justify-between px-2.5 py-1.5 rounded-md cursor-pointer transition-all duration-200 relative
          hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 hover:shadow-sm
          ${isActive ? 'bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-700 shadow-sm ring-1 ring-blue-200' : 'text-gray-700'}
          ${isChildActive && !isActive ? 'text-blue-600 bg-blue-25' : ''}
        `}
        style={{ paddingLeft }}
        onClick={handleClick}
      >
        {/* 活动状态指示器 */}
        {isActive && (
          <div className="absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-4 bg-gradient-to-b from-blue-500 to-indigo-600 rounded-full shadow-sm" />
        )}
        
        <div className="flex items-center flex-1 min-w-0">
          {hasChildren && (
            <button
              onClick={toggleExpanded}
              className="flex-shrink-0 mr-1.5 p-0.5 rounded hover:bg-blue-100 transition-all duration-200"
            >
              {isExpanded ? (
                <ChevronDown className="w-3 h-3 text-gray-500" />
              ) : (
                <ChevronRight className="w-3 h-3 text-gray-500" />
              )}
            </button>
          )}
          <span 
            className={`
              text-sm truncate transition-all duration-200
              ${isActive ? 'font-semibold text-blue-800' : level === 1 ? 'font-medium' : 'font-normal'}
              ${!isActive ? 'group-hover:font-medium group-hover:text-blue-700' : ''}
            `}
            title={item.title}
          >
            {item.title}
          </span>
        </div>
        
        {/* 层级指示器 */}
        {level === 1 && (
          <div className={`
            w-1 h-1 rounded-full transition-all duration-200 ml-1
            ${isActive ? 'bg-blue-600 shadow-sm' : 'bg-gray-300 group-hover:bg-blue-400'}
          `} />
        )}
      </div>
      
      {hasChildren && isExpanded && (
        <div className="ml-1 border-l border-blue-100 pl-1.5 space-y-0.5 mt-0.5">
          {item.children!.map((child) => (
            <TOCItem
              key={child.id}
              item={child}
              activeId={activeId}
              onItemClick={onItemClick}
              level={level + 1}
            />
          ))}
        </div>
      )}
    </div>
  )
}

const TableOfContents: React.FC<TableOfContentsProps> = ({
  items,
  activeId,
  onItemClick
}) => {
  const [isCollapsed, setIsCollapsed] = useState(false)

  // 如果没有目录项，不渲染组件
  if (!items || items.length === 0) {
    return (
      <div className="p-4 text-center">
        <List className="w-8 h-8 text-gray-300 mx-auto mb-2" />
        <p className="text-sm text-gray-500">暂无目录</p>
      </div>
    )
  }

  return (
    <div className="h-full bg-gradient-to-b from-white via-blue-50/30 to-indigo-50/20 border-r border-blue-100/60 shadow-inner">
      {/* 精简的目录头部 */}
      <div className="sticky top-0 z-10 bg-gradient-to-r from-white/98 to-blue-50/95 backdrop-blur-md border-b border-blue-100/50 px-3 py-2 shadow-sm">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="p-1 rounded-md bg-gradient-to-br from-blue-500 to-indigo-600 shadow-sm">
              <BookOpen className="w-3 h-3 text-white" />
            </div>
            <span className="text-sm font-semibold text-gray-800">目录</span>
            <span className="text-xs text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded-md font-medium">
              {items.length}
            </span>
          </div>
          <button
            onClick={() => setIsCollapsed(!isCollapsed)}
            className="p-1 rounded-md hover:bg-blue-50 transition-all duration-200 hover:shadow-sm"
            title={isCollapsed ? "展开目录" : "收起目录"}
          >
            {isCollapsed ? (
              <ChevronRight className="w-3.5 h-3.5 text-gray-600" />
            ) : (
              <ChevronDown className="w-3.5 h-3.5 text-gray-600" />
            )}
          </button>
        </div>
      </div>

      {/* 目录内容 - 紧凑布局 */}
      {!isCollapsed && (
        <div className="px-2 py-1 space-y-0.5 overflow-y-auto" style={{ height: 'calc(100% - 42px)' }}>
          {items.map((item) => (
            <TOCItem
              key={item.id}
              item={item}
              activeId={activeId}
              onItemClick={onItemClick}
              level={1}
            />
          ))}
        </div>
      )}

      {/* 收起状态的简化显示 */}
      {isCollapsed && (
        <div className="px-3 py-2">
          <div className="text-center text-xs text-gray-400 bg-gray-50 rounded-md py-1">
            目录已收起
          </div>
        </div>
      )}
    </div>
  )
}

export default TableOfContents
