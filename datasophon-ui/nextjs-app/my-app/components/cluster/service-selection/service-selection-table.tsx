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
  selectedServiceIds: string[]
  onToggleService: (serviceId: string) => void
  isServiceDisabled?: (service: Service) => boolean
}

const ServiceSelectionTable: React.FC<ServiceSelectionTableProps> = ({
  table,
  loading = false,
  selectedServiceIds,
  onToggleService,
  isServiceDisabled
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
                const isDisabled = isServiceDisabled?.(row.original) || false
                
                return (
                  <TableRow
                    key={row.id}
                    data-state={row.getIsSelected() && "selected"}
                    onClick={() => {
                      if (!isDisabled) {
                        onToggleService(row.original.id)
                      }
                    }}
                    className={`
                      transition-colors border-b border-gray-100
                      ${isDisabled 
                        ? 'cursor-not-allowed opacity-60 bg-gray-50' 
                        : 'cursor-pointer hover:bg-gray-50'
                      }
                      ${isSelected ? 'bg-blue-50/50' : ''}
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


    </div>
  )
}



// 卡片视图组件（移动端友好）
interface ServiceCardViewProps {
  services: Service[]
  selectedServiceIds: string[]
  onToggleService: (serviceId: string) => void
  isServiceDisabled?: (service: Service) => boolean
  loading?: boolean
}

export const ServiceCardView: React.FC<ServiceCardViewProps> = ({
  services,
  selectedServiceIds,
  onToggleService,
  isServiceDisabled,
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
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
      {services.map((service) => {
        const isSelected = selectedServiceIds.includes(service.id)
        const isDisabled = isServiceDisabled?.(service) || false
        
        return (
          <div
            key={service.id}
            className={`
              relative p-3 rounded-3xl border-2 transition-all duration-200 
              ${isDisabled 
                ? 'cursor-not-allowed opacity-60 bg-gray-50 border-gray-300' 
                : 'cursor-pointer hover:scale-105'
              }
              ${isSelected && !isDisabled
                ? 'border-blue-500 bg-blue-50 shadow-lg shadow-blue-500/20'
                : !isDisabled 
                  ? 'border-gray-200 bg-white hover:border-blue-300 hover:shadow-md'
                  : ''
              }
            `}
            onClick={() => {
              if (!isDisabled) {
                onToggleService(service.id)
              }
            }}
          >
            {/* 状态标识 */}
            <div className="absolute top-2 right-2 flex flex-col gap-1">
              {service.isRequired && (
                <span 
                  className="px-2 py-0.5 bg-red-500 text-white text-xs font-medium rounded-full"
                  title={`必需服务 - isRequired: ${service.isRequired} (${typeof service.isRequired})`}
                >
                  必需
                </span>
              )}
              {service.installed && (
                <span className="px-2 py-0.5 bg-green-500 text-white text-xs font-medium rounded-full">
                  已装
                </span>
              )}
            </div>

            {/* 服务图标 */}
            <div className="flex justify-center mb-2">
              <div className={`w-10 h-10 rounded-2xl flex items-center justify-center transition-colors ${
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
            <div className="text-center space-y-1">
              <h3 className={`font-semibold text-sm truncate ${
                isSelected ? 'text-blue-900' : 'text-gray-900'
              }`}>
                {service.serviceName}
              </h3>
              
              {service.serviceDesc && (
                <p className={`text-xs line-clamp-1 ${
                  isSelected ? 'text-blue-700' : 'text-gray-600'
                }`}>
                  {service.serviceDesc}
                </p>
              )}

              {/* 版本信息 */}
              {service.version && (
                <span className="inline-block px-1.5 py-0.5 bg-gray-100 text-gray-700 text-xs rounded-full">
                  v{service.version}
                </span>
              )}
            </div>

            {/* 选择状态指示器 */}
            <div className="absolute bottom-2 right-2">
              <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center transition-all ${
                isSelected
                  ? 'border-blue-500 bg-blue-500'
                  : 'border-gray-300'
              }`}>
                {isSelected && (
                  <svg className="w-2.5 h-2.5 text-white" fill="currentColor" viewBox="0 0 20 20">
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
