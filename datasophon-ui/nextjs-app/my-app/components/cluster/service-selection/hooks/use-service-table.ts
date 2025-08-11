"use client"

import { useState, useMemo } from 'react'
import { 
  getCoreRowModel, 
  useReactTable,
  ColumnDef,
  PaginationState,
  SortingState,
  getSortedRowModel,
  getPaginationRowModel
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
  pagination: PaginationState
  setPagination: (updater: PaginationState | ((old: PaginationState) => PaginationState)) => void
}

export const useServiceTable = ({
  services
}: UseServiceTableOptions): UseServiceTableReturn => {
  // 分页状态
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: 10
  })

  // 排序状态
  const [sorting, setSorting] = useState<SortingState>([])

  // 简化的列定义 - 移除JSX元素
  const columns = useMemo<ColumnDef<Service>[]>(() => [
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
  ], [])

  // 创建表格实例
  const table = useReactTable({
    data: services,
    columns,
    state: {
      pagination,
      sorting
    },
    onPaginationChange: setPagination,
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    manualPagination: false,
    manualSorting: false
  })

  return {
    table,
    pagination,
    setPagination
  }
}

export default useServiceTable