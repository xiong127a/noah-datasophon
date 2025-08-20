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
          group flex items-center justify-between px-3 py-2 rounded-lg cursor-pointer transition-all duration-200
          hover:bg-blue-50 hover:shadow-sm
          ${isActive ? 'bg-blue-100 text-blue-700 shadow-sm border-l-3 border-blue-500' : 'text-gray-700'}
          ${isChildActive && !isActive ? 'text-blue-600 bg-blue-25' : ''}
        `}
        style={{ paddingLeft }}
        onClick={handleClick}
      >
        <div className="flex items-center flex-1 min-w-0">
          {hasChildren && (
            <button
              onClick={toggleExpanded}
              className="flex-shrink-0 mr-2 p-0.5 rounded hover:bg-blue-100 transition-colors"
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
              text-sm font-medium truncate transition-all duration-200
              ${isActive ? 'font-semibold' : 'group-hover:font-medium'}
            `}
            title={item.title}
          >
            {item.title}
          </span>
        </div>
        
        {level === 1 && (
          <div className={`
            w-1.5 h-1.5 rounded-full transition-all duration-200
            ${isActive ? 'bg-blue-500' : 'bg-gray-300 group-hover:bg-blue-400'}
          `} />
        )}
      </div>
      
      {hasChildren && isExpanded && (
        <div className="ml-2 border-l border-gray-200 pl-2 space-y-1">
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
    <div className="h-full bg-gradient-to-b from-slate-50/80 to-blue-50/40 border-r border-slate-200/50">
      {/* 目录头部 */}
      <div className="sticky top-0 z-10 bg-gradient-to-r from-white/95 to-blue-50/60 backdrop-blur-sm border-b border-slate-200/50 px-4 py-4 shadow-sm">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-1.5 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 shadow-sm">
              <BookOpen className="w-4 h-4 text-white" />
            </div>
            <h3 className="font-bold text-gray-900">目录导航</h3>
            <span className="text-xs text-blue-700 bg-blue-100 px-2.5 py-1 rounded-full font-semibold shadow-sm">
              {items.length}
            </span>
          </div>
          <button
            onClick={() => setIsCollapsed(!isCollapsed)}
            className="p-1 rounded hover:bg-gray-100 transition-colors"
            title={isCollapsed ? "展开目录" : "收起目录"}
          >
            {isCollapsed ? (
              <ChevronRight className="w-4 h-4 text-gray-500" />
            ) : (
              <ChevronDown className="w-4 h-4 text-gray-500" />
            )}
          </button>
        </div>
      </div>

      {/* 目录内容 */}
      {!isCollapsed && (
        <div className="p-3 space-y-1 overflow-y-auto max-h-[calc(100vh-120px)]">
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
        <div className="p-3">
          <div className="text-center text-xs text-gray-500">
            目录已收起
          </div>
        </div>
      )}
    </div>
  )
}

export default TableOfContents
