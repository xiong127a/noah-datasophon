"use client"

import { useState } from 'react'
import { Server } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { toast } from 'sonner'
import { AddRackRequest } from '../../types/rack'
import { clusterApi } from "@/lib/api"

interface AddRackDialogProps {
  open: boolean
  onCancel: () => void
  onSuccess: () => void
  clusterId: number
}

const AddRackDialog = ({ open, onCancel, onSuccess, clusterId }: AddRackDialogProps) => {
  const [loading, setLoading] = useState(false)
  const [rackName, setRackName] = useState('')
  const [errors, setErrors] = useState<{ rackName?: string }>({})

  const validateRackName = (value: string): string | null => {
    if (!value) {
      return '机架名称不能为空!'
    }

    // 检查是否包含中文
    const chineseRegex = /[\u4E00-\u9FA5]|[\uFE30-\uFFA0]/g
    if (chineseRegex.test(value)) {
      return '名称中不能包含中文'
    }

    // 检查是否包含空格
    if (/\s/g.test(value)) {
      return '名称中不能包含空格'
    }

    return null
  }

  const handleSubmit = async () => {
    // 验证输入
    const rackNameError = validateRackName(rackName)
    if (rackNameError) {
      setErrors({ rackName: rackNameError })
      return
    }

    setErrors({})
    setLoading(true)

    try {
      const response = await clusterApi.rack.save(rackName)

      if (response.data.code === 200) {
        toast.success('保存成功')
        handleCancel()
        onSuccess()
      } else {
        toast.error(response.data.msg || '保存失败')
      }
    } catch (error) {
      console.error('保存机架失败:', error)
      toast.error('保存失败，请检查网络连接')
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    setRackName('')
    setErrors({})
    onCancel()
  }

  const handleInputChange = (value: string) => {
    setRackName(value)
    // 清除错误信息
    if (errors.rackName) {
      const error = validateRackName(value)
      setErrors({ rackName: error || undefined })
    }
  }

  return (
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent className="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <div className="p-2 bg-blue-100 rounded-lg">
              <Server className="h-4 w-4 text-blue-600" />
            </div>
            <span>添加机架</span>
          </DialogTitle>
          <DialogDescription>
            在当前集群中添加新的机架配置
          </DialogDescription>
        </DialogHeader>
        
        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="rackName">机架名称</Label>
            <Input
              id="rackName"
              placeholder="请输入机架名称"
              value={rackName}
              onChange={(e) => handleInputChange(e.target.value)}
              className={errors.rackName ? 'border-red-500' : ''}
            />
            {errors.rackName && (
              <p className="text-sm text-red-500">{errors.rackName}</p>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleCancel}>
            取消
          </Button>
          <Button onClick={handleSubmit} disabled={loading}>
            {loading ? '保存中...' : '确认'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export default AddRackDialog