"use client"

import { useState, useMemo } from 'react'
import { 
  getCoreRowModel, 
  useReactTable,
  ColumnDef,
  SortingState,
  getSortedRowModel
} from '@tanstack/react-table'
import type { Service } from '@/types/service-selection'

/**
 * 简化的服务表格Hook
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

interface UseServiceTableOptions {
  services: Service[]
  selectedServiceIds?: number[]  // 可选参数
  onToggleService?: (serviceId: number) => void  // 可选参数
}

interface UseServiceTableReturn {
  table: ReturnType<typeof useReactTable<Service>>
}

export const useServiceTable = ({
  services,
  selectedServiceIds = []
}: UseServiceTableOptions): UseServiceTableReturn => {
  // 排序状态
  const [sorting, setSorting] = useState<SortingState>([])

  // 包含勾选功能的列定义
  const columns = useMemo<ColumnDef<Service>[]>(() => [
    {
      id: 'select',
      header: '选择',
      cell: ({ row }) => {
        const isSelected = selectedServiceIds.includes(row.original.id)
        return isSelected ? '✓' : ''
      },
      size: 80
    },
    {
      accessorKey: 'serviceName',
      header: '服务名称',
      cell: info => info.getValue() as string
    },
    {
      accessorKey: 'serviceDesc',
      header: '描述',
      cell: info => (info.getValue() as string) || '-'
    },
    {
      accessorKey: 'isRequired',
      header: '类型',
      cell: info => (info.getValue() as boolean) ? '必需' : '可选'
    }
  ], [selectedServiceIds])

  // 创建表格实例（无分页）
  const table = useReactTable({
    data: services,
    columns,
    state: {
      sorting
    },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    manualSorting: false
  })

  return {
    table
  }
}

export default useServiceTable