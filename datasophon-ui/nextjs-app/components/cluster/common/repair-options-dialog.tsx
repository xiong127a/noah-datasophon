'use client'

import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Loader2 } from 'lucide-react'
import { clusterApiV1 } from '@/lib/api-utils-v1'

interface RepairOptionsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  checkName: string
  onConfirm: (options: RepairOptions) => void
}

export interface RepairOptions {
  jdkPackage?: string
  isArm?: boolean
}

interface JdkVersionInfo {
  version: string
  displayName: string
  filename: string
  description: string
}

interface JdkConfig {
  advancedSelectionEnabled: boolean
  defaultVersion: string
  defaultJdkInfo?: {
    displayName: string
    filename: string
  }
  availableVersions?: JdkVersionInfo[]
}

export function RepairOptionsDialog({
  open,
  onOpenChange,
  checkName,
  onConfirm
}: RepairOptionsDialogProps) {
  const [jdkConfig, setJdkConfig] = useState<JdkConfig | null>(null)
  const [selectedJdkVersion, setSelectedJdkVersion] = useState<string>('')
  const [isArmArchitecture, setIsArmArchitecture] = useState(false)
  const [loading, setLoading] = useState(true)

  // 获取JDK配置
  useEffect(() => {
    if (open) {
      loadJdkConfig()
    }
  }, [open])

  const loadJdkConfig = async () => {
    try {
      setLoading(true)
      const response = await clusterApiV1.environmentCheck.getJdkConfig()
      
      if (response.code === 200 && response.data) {
        setJdkConfig(response.data)
        
        // 如果启用高级选择且有可用版本，默认选择第一个
        if (response.data.advancedSelectionEnabled && response.data.availableVersions?.length > 0) {
          setSelectedJdkVersion(response.data.availableVersions[0].version)
        }
      }
    } catch (error) {
      console.error('获取JDK配置失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleConfirm = () => {
    const options: RepairOptions = {}
    
    // 如果启用高级选择，传递用户选择的JDK版本
    if (jdkConfig?.advancedSelectionEnabled && selectedJdkVersion) {
      const selectedVersion = jdkConfig.availableVersions?.find(v => v.version === selectedJdkVersion)
      if (selectedVersion) {
        // 构建JDK包路径：jdk/文件名
        const filename = isArmArchitecture 
          ? selectedVersion.filename.replace(/\.tar\.gz$/, '-arm.tar.gz').replace(/\.tgz$/, '-arm.tgz')
          : selectedVersion.filename
        options.jdkPackage = `jdk/${filename}`
        options.isArm = isArmArchitecture
      }
    }
    
    onConfirm(options)
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[550px]">
        <DialogHeader>
          <DialogTitle>JDK 修复选项</DialogTitle>
          <DialogDescription>
            选择 JDK 安装和配置选项
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-blue-500" />
            <span className="ml-2 text-sm text-gray-600">加载配置中...</span>
          </div>
        ) : (
          <div className="space-y-4 py-4">
            {/* JDK版本选择（仅在高级模式下显示） */}
            {jdkConfig?.advancedSelectionEnabled && jdkConfig.availableVersions && jdkConfig.availableVersions.length > 0 && (
              <div className="rounded-lg border border-indigo-200 bg-indigo-50 p-4">
                <h4 className="font-semibold text-indigo-900 mb-3">📦 JDK 版本选择</h4>
                
                <div className="space-y-3">
                  <div className="space-y-2">
                    <Label htmlFor="jdk-version" className="text-sm font-medium">
                      选择 JDK 版本
                    </Label>
                    <Select value={selectedJdkVersion} onValueChange={setSelectedJdkVersion}>
                      <SelectTrigger id="jdk-version">
                        <SelectValue placeholder="选择JDK版本" />
                      </SelectTrigger>
                      <SelectContent>
                        {jdkConfig.availableVersions.map((version) => (
                          <SelectItem key={version.version} value={version.version}>
                            <div className="flex flex-col">
                              <span className="font-medium">{version.displayName}</span>
                              <span className="text-xs text-gray-500">{version.description}</span>
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="flex items-start space-x-3">
                    <Checkbox
                      id="armArchitecture"
                      checked={isArmArchitecture}
                      onCheckedChange={(checked) => setIsArmArchitecture(checked as boolean)}
                    />
                    <div className="flex-1">
                      <Label
                        htmlFor="armArchitecture"
                        className="text-sm font-medium leading-none cursor-pointer"
                      >
                        ARM 架构
                      </Label>
                      <p className="text-xs text-gray-500 mt-1">
                        勾选此项如果目标主机是 ARM 架构（如 aarch64）
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 默认JDK版本提示（非高级模式） */}
            {!jdkConfig?.advancedSelectionEnabled && jdkConfig?.defaultJdkInfo && (
              <div className="rounded-lg border border-blue-200 bg-blue-50 p-4">
                <h4 className="font-semibold text-blue-900 mb-2">📦 JDK 版本</h4>
                <p className="text-sm text-blue-800">
                  将安装: <strong>{jdkConfig.defaultJdkInfo.displayName}</strong>
                </p>
                <p className="text-xs text-blue-600 mt-1">
                  {jdkConfig.defaultJdkInfo.filename}
                </p>
              </div>
            )}

            {/* 必选项说明 */}
            <div className="rounded-lg border border-blue-200 bg-blue-50 p-4">
              <h4 className="font-semibold text-blue-900 mb-2">✅ 默认安装内容</h4>
              <ul className="text-sm text-blue-800 space-y-1 ml-4 list-disc">
                <li>下载并解压 JDK 安装包</li>
                <li>配置环境变量（通过 /etc/profile.d/datasophon-env.sh）</li>
                <li>设置 JAVA_HOME、PATH 等环境变量</li>
              </ul>
            </div>
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading}>
            取消
          </Button>
          <Button onClick={handleConfirm} disabled={loading}>
            开始修复
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

