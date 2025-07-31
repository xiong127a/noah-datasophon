"use client"

import React, { useState, useEffect } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../ui/dialog"
import { Button } from "../ui/button"
import { Input } from "../ui/input"
import { Label } from "../ui/label"
import { Alert, AlertDescription } from "../ui/alert"
import { Loader2, AlertCircle } from "lucide-react"
import { CreateTagRequest, TagOperationResponse } from "../../types/tag"

interface AddTagDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  clusterId: number
}

export default function AddTagDialog({ open, onClose, onSuccess, clusterId }: AddTagDialogProps) {
  const [tagName, setTagName] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 重置表单
  const resetForm = () => {
    setTagName("")
    setError(null)
  }

  // 表单验证
  const validateForm = (): string | null => {
    if (!tagName.trim()) {
      return "标签名称不能为空！"
    }

    // 检查是否包含中文
    const chineseRegex = /[\u4E00-\u9FA5]|[\uFE30-\uFFA0]/
    if (chineseRegex.test(tagName)) {
      return "名称中不能包含中文"
    }

    // 检查是否包含空格
    if (/\s/.test(tagName)) {
      return "名称中不能包含空格"
    }

    return null
  }

  // 提交表单
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    const validationError = validateForm()
    if (validationError) {
      setError(validationError)
      return
    }

    setLoading(true)
    setError(null)

    try {
      const requestData: CreateTagRequest = {
        nodeLabel: tagName.trim(),
        clusterId: clusterId
      }

      const response = await fetch('/api/cluster/node/label/save', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData),
      })

      const result: TagOperationResponse = await response.json()

      if (result.code === 200) {
        resetForm()
        onSuccess()
        onClose()
      } else {
        setError(result.message || '保存失败，请重试')
      }
    } catch (error) {
      console.error('保存标签失败:', error)
      setError('网络错误，请重试')
    } finally {
      setLoading(false)
    }
  }

  // 关闭对话框
  const handleClose = () => {
    if (!loading) {
      resetForm()
      onClose()
    }
  }

  // 键盘事件处理
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !loading) {
      handleSubmit(e)
    }
  }

  // 当对话框打开时重置表单
  useEffect(() => {
    if (open) {
      resetForm()
    }
  }, [open])

  return (
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-lg font-semibold">添加标签</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* 错误提示 */}
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* 标签名称输入 */}
          <div className="space-y-2">
            <Label htmlFor="tagName" className="text-sm font-medium">
              标签名称 <span className="text-red-500">*</span>
            </Label>
            <Input
              id="tagName"
              type="text"
              value={tagName}
              onChange={(e) => {
                setTagName(e.target.value)
                if (error) setError(null) // 清除错误信息
              }}
              onKeyPress={handleKeyPress}
              placeholder="请输入标签名称"
              disabled={loading}
              className="w-full"
              autoFocus
            />
            <p className="text-xs text-gray-500">
              注意：标签名称不能包含中文和空格
            </p>
          </div>

          <DialogFooter className="flex space-x-2 pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={loading}
              className="flex-1"
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={loading || !tagName.trim()}
              className="flex-1 bg-blue-600 hover:bg-blue-700"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  保存中...
                </>
              ) : (
                '确认'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}