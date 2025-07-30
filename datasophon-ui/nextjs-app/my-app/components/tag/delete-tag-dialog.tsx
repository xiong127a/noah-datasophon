"use client"

import React, { useState } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../ui/dialog"
import { Button } from "../ui/button"
import { Alert, AlertDescription } from "../ui/alert"
import { Badge } from "../ui/badge"
import { Loader2, AlertTriangle, Trash2 } from "lucide-react"
import { Tag, DeleteTagRequest, TagOperationResponse } from "../../types/tag"

interface DeleteTagDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  tag: Tag | null
}

export default function DeleteTagDialog({ open, onClose, onSuccess, tag }: DeleteTagDialogProps) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 确认删除
  const handleConfirmDelete = async () => {
    if (!tag) return

    setLoading(true)
    setError(null)

    try {
      const requestData: DeleteTagRequest = {
        nodeLabelId: tag.id
      }

      const response = await fetch('/api/cluster/node/label/delete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData),
      })

      const result: TagOperationResponse = await response.json()

      if (result.code === 200) {
        onSuccess()
        onClose()
      } else {
        setError(result.message || '删除失败，请重试')
      }
    } catch (error) {
      console.error('删除标签失败:', error)
      setError('网络错误，请重试')
    } finally {
      setLoading(false)
    }
  }

  // 关闭对话框
  const handleClose = () => {
    if (!loading) {
      setError(null)
      onClose()
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center text-lg font-semibold text-orange-700">
            <AlertTriangle className="w-5 h-5 mr-2 text-orange-600" />
            提示
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* 错误提示 */}
          {error && (
            <Alert variant="destructive">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* 确认删除信息 */}
          <div className="space-y-3">
            <p className="text-gray-700">
              确认删除当前标签？
            </p>
            
            {tag && (
              <div className="p-3 bg-gray-50 rounded-lg border">
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-gray-600">标签名称：</span>
                  <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
                    {tag.nodeLabel}
                  </Badge>
                </div>
              </div>
            )}

            <div className="flex items-start space-x-2 p-3 bg-orange-50 rounded-lg border border-orange-200">
              <AlertTriangle className="w-4 h-4 text-orange-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm text-orange-700">
                <p className="font-medium">注意：</p>
                <p>删除后将无法恢复，请确认是否继续？</p>
              </div>
            </div>
          </div>
        </div>

        <DialogFooter className="flex space-x-2">
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
            type="button"
            variant="destructive"
            onClick={handleConfirmDelete}
            disabled={loading}
            className="flex-1"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                删除中...
              </>
            ) : (
              <>
                <Trash2 className="w-4 h-4 mr-2" />
                确定删除
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}