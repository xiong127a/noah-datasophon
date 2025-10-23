/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Dashboard美化分页组件
 */

"use client"

import React from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { 
  ChevronLeft, 
  ChevronRight, 
  ChevronsLeft, 
  ChevronsRight,
  Database,
  Loader2,
  FileText
} from "lucide-react";

interface KubernetesPaginationProps {
  /** 当前页码 */
  currentPage: number;
  /** 每页显示数量 */
  pageSize: number;
  /** 总记录数 */
  total: number;
  /** 页码变化回调 */
  onPageChange: (page: number) => void;
  /** 页大小变化回调 */
  onPageSizeChange: (pageSize: number) => void;
  /** 是否显示页大小选择器 */
  showPageSizeSelector?: boolean;
  /** 是否加载中 */
  loading?: boolean;
  /** 自定义className */
  className?: string;
}

const KubernetesPagination: React.FC<KubernetesPaginationProps> = ({
  currentPage,
  pageSize,
  total,
  onPageChange,
  onPageSizeChange,
  showPageSizeSelector = true,
  loading = false,
  className = ""
}) => {
  // 计算总页数
  const totalPages = Math.ceil(total / pageSize);
  
  // 计算当前显示范围
  const startItem = Math.max(1, (currentPage - 1) * pageSize + 1);
  const endItem = Math.min(total, currentPage * pageSize);

  // 生成页码列表
  const generatePageNumbers = () => {
    const pages = [];
    const delta = 2; // 当前页前后显示的页码数
    
    if (totalPages <= 7) {
      // 如果总页数小于等于7，显示所有页码
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      // 复杂分页逻辑
      pages.push(1); // 总是显示第一页
      
      if (currentPage <= 4) {
        // 当前页在前面，显示 1 2 3 4 5 ... last
        for (let i = 2; i <= Math.min(5, totalPages - 1); i++) {
          pages.push(i);
        }
        if (totalPages > 6) {
          pages.push('ellipsis-end');
        }
      } else if (currentPage >= totalPages - 3) {
        // 当前页在后面，显示 1 ... (last-4) (last-3) (last-2) (last-1) last
        if (totalPages > 6) {
          pages.push('ellipsis-start');
        }
        for (let i = Math.max(2, totalPages - 4); i <= totalPages - 1; i++) {
          pages.push(i);
        }
      } else {
        // 当前页在中间，显示 1 ... (current-1) current (current+1) ... last
        pages.push('ellipsis-start');
        for (let i = currentPage - 1; i <= currentPage + 1; i++) {
          pages.push(i);
        }
        pages.push('ellipsis-end');
      }
      
      if (totalPages > 1) {
        pages.push(totalPages); // 总是显示最后一页
      }
    }
    
    return pages;
  };

  if (total === 0) {
    return null;
  }

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className={`bg-gradient-to-r from-white to-gray-50 border border-gray-200 rounded-xl shadow-sm p-4 ${className}`}
    >
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between space-y-4 lg:space-y-0">
        {/* 左侧：显示信息和页大小选择 */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:space-x-6 space-y-3 sm:space-y-0">
          {/* 数据统计信息 */}
          <motion.div 
            className="flex items-center space-x-2 text-sm"
            whileHover={{ scale: 1.02 }}
          >
            <div className="flex items-center space-x-1.5 text-gray-600">
              <Database className="w-4 h-4 text-blue-500" />
              <span className="font-medium">数据统计</span>
            </div>
            <div className="h-4 w-px bg-gray-300"></div>
            <div className="text-gray-700">
              显示 <span className="font-semibold text-blue-600 px-1 py-0.5 bg-blue-50 rounded">{startItem}</span> 到{" "}
              <span className="font-semibold text-blue-600 px-1 py-0.5 bg-blue-50 rounded">{endItem}</span> 条
            </div>
            <div className="text-gray-500">•</div>
            <div className="flex items-center space-x-1 text-gray-700">
              <FileText className="w-3.5 h-3.5 text-green-500" />
              <span>共</span>
              <span className="font-semibold text-green-600 px-2 py-0.5 bg-green-50 rounded-md">{total}</span>
              <span>条记录</span>
            </div>
          </motion.div>
          
          {/* 页大小选择器 */}
          {showPageSizeSelector && (
            <motion.div 
              className="flex items-center space-x-2"
              whileHover={{ scale: 1.02 }}
            >
              <div className="flex items-center space-x-1.5 text-sm text-gray-600">
                <span className="font-medium">每页显示</span>
              </div>
              <Select
                value={pageSize.toString()}
                onValueChange={(value) => onPageSizeChange(parseInt(value))}
                disabled={loading}
              >
                <SelectTrigger className="h-8 w-20 border-gray-200 hover:border-blue-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition-colors">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="10" className="hover:bg-blue-50">10</SelectItem>
                  <SelectItem value="20" className="hover:bg-blue-50">20</SelectItem>
                  <SelectItem value="50" className="hover:bg-blue-50">50</SelectItem>
                  <SelectItem value="100" className="hover:bg-blue-50">100</SelectItem>
                </SelectContent>
              </Select>
              <span className="text-sm text-gray-600">条/页</span>
            </motion.div>
          )}
        </div>

        {/* 右侧：分页导航 */}
        {totalPages > 1 && (
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
            className="flex justify-center lg:justify-end"
          >
            <div className="flex items-center space-x-1 bg-white border border-gray-200 rounded-lg p-1 shadow-sm">
              {/* 快速跳转到第一页 */}
              {currentPage > 3 && (
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => !loading && onPageChange(1)}
                  disabled={loading}
                  className="flex items-center justify-center w-8 h-8 rounded-md text-gray-500 hover:text-blue-600 hover:bg-blue-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <ChevronsLeft className="w-4 h-4" />
                </motion.button>
              )}

              {/* 上一页 */}
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => currentPage > 1 && !loading && onPageChange(currentPage - 1)}
                disabled={currentPage <= 1 || loading}
                className="flex items-center justify-center w-8 h-8 rounded-md text-gray-500 hover:text-blue-600 hover:bg-blue-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ChevronLeft className="w-4 h-4" />
              </motion.button>

              {/* 页码按钮 */}
              <div className="flex items-center space-x-0.5">
                <AnimatePresence>
                  {generatePageNumbers().map((page, index) => (
                    <motion.div
                      key={`${page}-${index}`}
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.8 }}
                      transition={{ duration: 0.15 }}
                    >
                      {page === 'ellipsis-start' || page === 'ellipsis-end' ? (
                        <div className="flex items-center justify-center w-8 h-8 text-gray-400">
                          <span className="text-sm">•••</span>
                        </div>
                      ) : (
                        <motion.button
                          whileHover={{ scale: 1.1 }}
                          whileTap={{ scale: 0.9 }}
                          onClick={() => page !== currentPage && !loading && onPageChange(page as number)}
                          disabled={loading}
                          className={`
                            flex items-center justify-center w-8 h-8 rounded-md text-sm font-medium transition-all duration-200
                            ${page === currentPage 
                              ? 'bg-blue-600 text-white shadow-md ring-2 ring-blue-100' 
                              : 'text-gray-700 hover:text-blue-600 hover:bg-blue-50'
                            }
                            ${loading ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                          `}
                        >
                          {page}
                        </motion.button>
                      )}
                    </motion.div>
                  ))}
                </AnimatePresence>
              </div>

              {/* 下一页 */}
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => currentPage < totalPages && !loading && onPageChange(currentPage + 1)}
                disabled={currentPage >= totalPages || loading}
                className="flex items-center justify-center w-8 h-8 rounded-md text-gray-500 hover:text-blue-600 hover:bg-blue-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ChevronRight className="w-4 h-4" />
              </motion.button>

              {/* 快速跳转到最后一页 */}
              {currentPage < totalPages - 2 && (
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => !loading && onPageChange(totalPages)}
                  disabled={loading}
                  className="flex items-center justify-center w-8 h-8 rounded-md text-gray-500 hover:text-blue-600 hover:bg-blue-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <ChevronsRight className="w-4 h-4" />
                </motion.button>
              )}

              {/* 加载指示器 */}
              {loading && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="flex items-center justify-center w-8 h-8"
                >
                  <Loader2 className="w-4 h-4 animate-spin text-blue-500" />
                </motion.div>
              )}
            </div>
          </motion.div>
        )}
      </div>

      {/* 小屏幕下的简化页面信息 */}
      <div className="lg:hidden mt-3 flex items-center justify-center">
        <div className="text-xs text-gray-500 bg-gray-50 px-3 py-1 rounded-full">
          第 <span className="font-medium text-blue-600">{currentPage}</span> 页 / 共 <span className="font-medium text-blue-600">{totalPages}</span> 页
        </div>
      </div>
    </motion.div>
  );
};

export default KubernetesPagination;
