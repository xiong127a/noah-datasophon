'use client'

import { useState } from 'react'
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
import { AlertTriangle } from 'lucide-react'

interface RepairOptionsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  checkName: string
  onConfirm: (options: RepairOptions) => void
}

export interface RepairOptions {
  createSymlinks: boolean
}

export function RepairOptionsDialog({
  open,
  onOpenChange,
  checkName,
  onConfirm
}: RepairOptionsDialogProps) {
  const [createSymlinks, setCreateSymlinks] = useState(false)

  const handleConfirm = () => {
    onConfirm({ createSymlinks })
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>JDK 修复选项</DialogTitle>
          <DialogDescription>
            选择 JDK 安装和配置选项
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* 必选项说明 */}
          <div className="rounded-lg border border-blue-200 bg-blue-50 p-4">
            <h4 className="font-semibold text-blue-900 mb-2">✅ 默认安装内容（必选）</h4>
            <ul className="text-sm text-blue-800 space-y-1 ml-4 list-disc">
              <li>下载并解压 JDK 安装包</li>
              <li>配置用户环境变量（~/.bashrc）</li>
              <li>设置 JAVA_HOME、PATH 等环境变量</li>
            </ul>
          </div>

          {/* 可选项 */}
          <div className="rounded-lg border border-gray-200 bg-gray-50 p-4">
            <h4 className="font-semibold text-gray-900 mb-3">⚙️ 可选配置</h4>
            
            <div className="flex items-start space-x-3">
              <Checkbox
                id="createSymlinks"
                checked={createSymlinks}
                onCheckedChange={(checked) => setCreateSymlinks(checked as boolean)}
              />
              <div className="flex-1">
                <Label
                  htmlFor="createSymlinks"
                  className="text-sm font-medium leading-none cursor-pointer"
                >
                  创建系统软链接（需要 sudo 权限）
                </Label>
                <p className="text-xs text-gray-500 mt-1">
                  在 /usr/bin 下创建 java 和 javac 软链接，方便全局访问
                </p>
                
                {createSymlinks && (
                  <div className="mt-2 flex items-start gap-2 text-xs text-amber-700 bg-amber-50 p-2 rounded">
                    <AlertTriangle className="h-4 w-4 mt-0.5 flex-shrink-0" />
                    <span>
                      <strong>注意：</strong>此选项需要 sudo 权限。如果您没有 root 权限，
                      软链接创建将失败，但不影响 JDK 正常使用（通过环境变量访问）。
                    </span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={handleConfirm}>
            开始修复
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

