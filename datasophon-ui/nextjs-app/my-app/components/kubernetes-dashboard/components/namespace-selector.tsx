/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes命名空间选择器组件
 */

"use client";

import React, { useState, useEffect, useCallback, useRef, memo } from "react";
import { 
  ChevronDown, 
  Search, 
  Loader2
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { ScrollArea } from "@/components/ui/scroll-area";

import { Namespace } from "../types";

interface NamespaceSelectorProps {
  clusterId: string;
  value: string;
  onChange: (namespace: string) => void;
  collapsed?: boolean;
  className?: string;
}

// 独立的下拉内容组件 - 使用memo避免不必要的重渲染
const NamespaceDropdownContent = memo<{
  searchTerm: string;
  onSearchChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  loading: boolean;
  filteredNamespaces: Namespace[];
  selectedValue: string;
  onNamespaceSelect: (name: string) => void;
  searchInputRef: React.RefObject<HTMLInputElement | null>;
}>(({
  searchTerm,
  onSearchChange,
  loading,
  filteredNamespaces,
  selectedValue,
  onNamespaceSelect,
  searchInputRef
}) => (
  <div className="p-4 space-y-4">
    {/* 搜索框 */}
    <div className="relative">
      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
      <Input
        ref={searchInputRef}
        placeholder="搜索命名空间..."
        className="pl-10"
        value={searchTerm}
        onChange={onSearchChange}
        autoComplete="off"
      />
    </div>

    {/* 命名空间列表 */}
    <ScrollArea className="h-96 overflow-auto">
      <div className="space-y-1 pr-2">
        {loading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="w-6 h-6 animate-spin text-gray-400" />
            <span className="ml-2 text-sm text-gray-500">加载中...</span>
          </div>
        ) : filteredNamespaces.length === 0 ? (
          <div className="py-8 text-center text-gray-500 text-sm">
            {searchTerm ? '未找到匹配的命名空间' : '暂无命名空间'}
          </div>
        ) : (
          filteredNamespaces.map((namespace) => {
            const isSelected = namespace.metadata.name === selectedValue;
            
            return (
              <div
                key={namespace.metadata.name}
                className={`p-2 rounded-md border cursor-pointer transition-all duration-200 ${
                  isSelected 
                    ? 'bg-blue-50 border-blue-200 shadow-sm'
                    : 'bg-white border-gray-200 hover:bg-gray-50 hover:border-gray-300'
                }`}
                onClick={() => onNamespaceSelect(namespace.metadata.name)}
              >
                <div className="flex items-center justify-between py-1">
                  {/* 左侧：命名空间名称 */}
                  <div className="flex-1 min-w-0">
                    <span className={`text-sm font-medium truncate block ${
                      isSelected ? 'text-blue-700' : 'text-gray-900'
                    }`}>
                      {namespace.metadata.name}
                    </span>
                  </div>
                  
                  {/* 右侧：状态指示器 */}
                  <div className="flex items-center space-x-2 flex-shrink-0">
                    {isSelected && (
                      <div className="w-2 h-2 bg-blue-500 rounded-full" />
                    )}
                    <div className={`w-2 h-2 rounded-full ${
                      namespace.status?.phase === 'Active' ? 'bg-green-400' : 'bg-yellow-400'
                    }`} />
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </ScrollArea>

    {/* 底部信息 */}
    <div className="border-t pt-3 text-center">
      <span className="text-xs text-gray-500">
        共 {filteredNamespaces.length} 个命名空间
      </span>
    </div>
  </div>
));

NamespaceDropdownContent.displayName = 'NamespaceDropdownContent';

const NamespaceSelector: React.FC<NamespaceSelectorProps> = ({
  clusterId,
  value,
  onChange,
  collapsed = false,
  className = ""
}) => {
  const [namespaces, setNamespaces] = useState<Namespace[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const searchInputRef = useRef<HTMLInputElement>(null);

  // 获取命名空间数据
  const fetchNamespaces = useCallback(async () => {
    if (!clusterId) return;
    
    console.log('🔄 开始获取命名空间列表，clusterId:', clusterId);
    setLoading(true);
    try {
      // 调用优化后的API获取命名空间数据（已含基础统计）
      const { KubernetesAPI } = await import('@/lib/kubernetes-api');
      console.log('📡 调用 KubernetesAPI.getNamespaces API...');
      const k8sNamespaces = await KubernetesAPI.getNamespaces(clusterId);
      console.log('✅ 获取命名空间成功，数量:', k8sNamespaces.length);
      
      // 转换为内部组件格式（保持兼容性）
      const convertedNamespaces: Namespace[] = k8sNamespaces.map(ns => ({
        apiVersion: "v1",
        kind: "Namespace",
        metadata: {
          name: ns.name,
          creationTimestamp: ns.creationTime,
          labels: {}
        },
        status: { phase: ns.phase as "Active" | "Terminating" }
      }));
      setNamespaces(convertedNamespaces);
      
      // 🚀 极简版本：无统计数据，性能最佳
      console.log('✅ 命名空间列表加载完成，性能极佳！无统计数据负担');
      
    } catch (error) {
      console.error('❌ 获取命名空间失败:', error);
      // 如果API调用失败，设置空数据而不是模拟数据
      setNamespaces([]);
    } finally {
      setLoading(false);
    }
  }, [clusterId]);

  useEffect(() => {
    fetchNamespaces();
  }, [clusterId, fetchNamespaces]);

  // 优化搜索处理函数
  const handleSearchChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  }, []);

  // 过滤命名空间
  const filteredNamespaces = namespaces.filter(ns =>
    ns.metadata.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // 处理命名空间选择
  const handleNamespaceSelect = useCallback((namespaceName: string) => {
    onChange(namespaceName);
    setOpen(false);
    setSearchTerm("");
  }, [onChange]);

  if (collapsed) {
    return (
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button 
            variant="ghost" 
            className={`w-12 h-12 p-0 justify-center ${className}`}
          >
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
              <span className="text-xs font-bold text-white">NS</span>
            </div>
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-80 p-0" align="start" side="right">
          <NamespaceDropdownContent
            searchTerm={searchTerm}
            onSearchChange={handleSearchChange}
            loading={loading}
            filteredNamespaces={filteredNamespaces}
            selectedValue={value}
            onNamespaceSelect={handleNamespaceSelect}
            searchInputRef={searchInputRef}
          />
        </PopoverContent>
      </Popover>
    );
  }



  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          className={`w-full justify-between h-auto p-3 ${className} ${
            open ? 'border-blue-300 shadow-sm' : ''
          }`}
        >
          <div className="flex items-center justify-between w-full">
            <div className="flex-1 min-w-0 text-left">
              <span className="font-medium text-gray-900 truncate block">
                {value}
              </span>
              <span className="text-xs text-gray-500">
                命名空间
              </span>
            </div>
            <div className="w-2 h-2 bg-blue-400 rounded-full flex-shrink-0 ml-2" />
          </div>
          <ChevronDown className={`w-4 h-4 text-gray-400 transition-transform duration-200 ${
            open ? 'rotate-180' : ''
          }`} />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0" align="start">
        <NamespaceDropdownContent
          searchTerm={searchTerm}
          onSearchChange={handleSearchChange}
          loading={loading}
          filteredNamespaces={filteredNamespaces}
          selectedValue={value}
          onNamespaceSelect={handleNamespaceSelect}
          searchInputRef={searchInputRef}
        />
      </PopoverContent>
    </Popover>
  );
};

export default NamespaceSelector;
