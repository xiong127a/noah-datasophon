"use client"

import React from 'react'
import { 
  flexRender,
  type Table as TanstackTable
} from '@tanstack/react-table'
import { 
  ChevronUp, 
  ChevronDown, 
  ChevronsUpDown,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Loader2
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow 
} from '@/components/ui/table'
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select'
import ServiceIcon from '@/components/ui/service-icon'
import type { Service } from '@/types/service-selection'

/**
 * 现代化服务选择表格组件（基于Tanstack Table）
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

interface ServiceSelectionTableProps {
  table: TanstackTable<Service>
  loading?: boolean
  selectedServiceIds: number[]
  onToggleService: (serviceId: number) => void
}

const ServiceSelectionTable: React.FC<ServiceSelectionTableProps> = ({
  table,
  loading = false,
  selectedServiceIds,
  onToggleService
}) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-3">
          <Loader2 className="w-5 h-5 animate-spin text-blue-500" />
          <span className="text-gray-600">正在加载服务列表...</span>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* 表格容器 */}
      <div className="border border-gray-200 rounded-lg overflow-hidden bg-white shadow-sm">
        <Table>
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id} className="bg-gray-50/50">
                {headerGroup.headers.map((header) => (
                  <TableHead 
                    key={header.id}
                    className="font-semibold text-gray-900 border-b border-gray-200"
                    style={{ width: header.getSize() }}
                  >
                    {header.isPlaceholder ? null : (
                      <div
                        className={
                          header.column.getCanSort()
                            ? 'cursor-pointer select-none flex items-center gap-2 hover:text-blue-600'
                            : 'flex items-center gap-2'
                        }
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        {flexRender(
                          header.column.columnDef.header,
                          header.getContext()
                        )}
                        {header.column.getCanSort() && (
                          <span className="text-gray-400">
                            {header.column.getIsSorted() === 'desc' ? (
                              <ChevronDown className="w-4 h-4" />
                            ) : header.column.getIsSorted() === 'asc' ? (
                              <ChevronUp className="w-4 h-4" />
                            ) : (
                              <ChevronsUpDown className="w-4 h-4" />
                            )}
                          </span>
                        )}
                      </div>
                    )}
                  </TableHead>
                ))}
              </TableRow>
            ))}
          </TableHeader>

          <TableBody>
            {table.getRowModel().rows?.length ? (
              table.getRowModel().rows.map((row) => {
                const isSelected = selectedServiceIds.includes(row.original.id)
                
                return (
                  <TableRow
                    key={row.id}
                    data-state={row.getIsSelected() && "selected"}
                    className={`
                      transition-colors hover:bg-gray-50 border-b border-gray-100
                      ${isSelected ? 'bg-blue-50/50' : ''}
                      ${row.original.isRequired ? 'bg-red-50/30' : ''}
                    `}
                  >
                    {row.getVisibleCells().map((cell) => (
                      <TableCell 
                        key={cell.id}
                        className="py-3"
                        style={{ width: cell.column.getSize() }}
                      >
                        {flexRender(
                          cell.column.columnDef.cell,
                          cell.getContext()
                        )}
                      </TableCell>
                    ))}
                  </TableRow>
                )
              })
            ) : (
              <TableRow>
                <TableCell
                  colSpan={table.getAllColumns().length}
                  className="h-24 text-center text-gray-500"
                >
                  暂无服务数据
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* 分页控制器 */}
      <TablePagination table={table} />
    </div>
  )
}

// 分页控制器组件
interface TablePaginationProps {
  table: TanstackTable<Service>
}

const TablePagination: React.FC<TablePaginationProps> = ({ table }) => {
  return (
    <div className="flex items-center justify-between px-2 py-4">
      {/* 左侧：行选择信息 */}
      <div className="flex items-center space-x-6 text-sm text-gray-600">
        <div>
          共 {table.getFilteredRowModel().rows.length} 条记录
        </div>
        <div>
          已选择 {table.getFilteredSelectedRowModel().rows.length} 条
        </div>
      </div>

      {/* 右侧：分页控制 */}
      <div className="flex items-center space-x-6">
        {/* 每页显示数量选择器 */}
        <div className="flex items-center space-x-2">
          <p className="text-sm font-medium text-gray-700">每页显示</p>
          <Select
            value={`${table.getState().pagination.pageSize}`}
            onValueChange={(value) => {
              table.setPageSize(Number(value))
            }}
          >
            <SelectTrigger className="h-8 w-[70px]">
              <SelectValue placeholder={table.getState().pagination.pageSize} />
            </SelectTrigger>
            <SelectContent side="top">
              {[5, 10, 20, 30, 50, 100].map((pageSize) => (
                <SelectItem key={pageSize} value={`${pageSize}`}>
                  {pageSize}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <p className="text-sm font-medium text-gray-700">条</p>
        </div>

        {/* 页码信息 */}
        <div className="flex items-center justify-center text-sm font-medium text-gray-700">
          第 {table.getState().pagination.pageIndex + 1} 页，
          共 {table.getPageCount()} 页
        </div>

        {/* 分页按钮 */}
        <div className="flex items-center space-x-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => table.setPageIndex(0)}
            disabled={!table.getCanPreviousPage()}
            className="h-8 w-8 p-0"
          >
            <span className="sr-only">跳转到第一页</span>
            <ChevronsLeft className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => table.previousPage()}
            disabled={!table.getCanPreviousPage()}
            className="h-8 w-8 p-0"
          >
            <span className="sr-only">上一页</span>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => table.nextPage()}
            disabled={!table.getCanNextPage()}
            className="h-8 w-8 p-0"
          >
            <span className="sr-only">下一页</span>
            <ChevronRight className="h-4 w-4" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => table.setPageIndex(table.getPageCount() - 1)}
            disabled={!table.getCanNextPage()}
            className="h-8 w-8 p-0"
          >
            <span className="sr-only">跳转到最后一页</span>
            <ChevronsRight className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  )
}

// 卡片视图组件（移动端友好）
interface ServiceCardViewProps {
  services: Service[]
  selectedServiceIds: number[]
  onToggleService: (serviceId: number) => void
  loading?: boolean
}

export const ServiceCardView: React.FC<ServiceCardViewProps> = ({
  services,
  selectedServiceIds,
  onToggleService,
  loading = false
}) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-3">
          <Loader2 className="w-5 h-5 animate-spin text-blue-500" />
          <span className="text-gray-600">正在加载服务列表...</span>
        </div>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      {services.map((service) => {
        const isSelected = selectedServiceIds.includes(service.id)
        
        return (
          <div
            key={service.id}
            className={`
              relative p-4 rounded-lg border-2 transition-all duration-200 cursor-pointer
              ${isSelected
                ? 'border-blue-500 bg-blue-50 shadow-lg shadow-blue-500/20'
                : 'border-gray-200 bg-white hover:border-blue-300 hover:shadow-md'
              }
              ${service.isRequired ? 'ring-2 ring-red-200' : ''}
            `}
            onClick={() => onToggleService(service.id)}
          >
            {/* 状态标识 */}
            <div className="absolute top-2 right-2 flex flex-col gap-1">
              {service.isRequired && (
                <span className="px-2 py-1 bg-red-500 text-white text-xs font-medium rounded">
                  必需
                </span>
              )}
              {service.installed && (
                <span className="px-2 py-1 bg-green-500 text-white text-xs font-medium rounded">
                  已装
                </span>
              )}
            </div>

            {/* 服务图标 */}
            <div className="flex justify-center mb-3">
              <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${
                isSelected
                  ? 'bg-blue-500 text-white'
                  : 'bg-gray-100 text-gray-600'
              }`}>
                <ServiceIcon 
                  serviceName={service.serviceName}
                  size={24}
                />
              </div>
            </div>

            {/* 服务信息 */}
            <div className="text-center space-y-2">
              <h3 className={`font-semibold truncate ${
                isSelected ? 'text-blue-900' : 'text-gray-900'
              }`}>
                {service.serviceName}
              </h3>
              
              {service.serviceDesc && (
                <p className={`text-sm line-clamp-2 ${
                  isSelected ? 'text-blue-700' : 'text-gray-600'
                }`}>
                  {service.serviceDesc}
                </p>
              )}

              {/* 版本信息 */}
              {service.version && (
                <span className="inline-block px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded">
                  v{service.version}
                </span>
              )}
            </div>

            {/* 选择状态指示器 */}
            <div className="absolute bottom-2 right-2">
              <div className={`w-5 h-5 rounded border-2 flex items-center justify-center ${
                isSelected
                  ? 'border-blue-500 bg-blue-500'
                  : 'border-gray-300'
              }`}>
                {isSelected && (
                  <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                )}
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default ServiceSelectionTable
