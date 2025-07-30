"use client"

import { useState } from 'react'
import { AlertTriangle, Server } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { toast } from 'sonner'
import { Rack } from '../../types/rack'
import { API_PATHS, api } from '@/lib/api-config'

interface DeleteRackDialogProps {
  open: boolean
  onCancel: () => void
  onSuccess: () => void
  rack: Rack | null
}

const DeleteRackDialog = ({ open, onCancel, onSuccess, rack }: DeleteRackDialogProps) => {
  const [loading, setLoading] = useState(false)

  const handleSubmit = async () => {
    if (!rack) return

    try {
      setLoading(true)

      const response = await api.post(API_PATHS.RACK_DELETE, {
        rackId: rack.id,
        clusterId: rack.clusterId
      })

      if (response.data.code === 200) {
        toast.success('删除成功')
        onSuccess()
      } else {
        toast.error(response.data.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除机架失败:', error)
      toast.error('删除失败，请检查网络连接')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={(open) => !open && onCancel()}>
      <AlertDialogContent className="sm:max-w-[400px]">
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center space-x-2">
            <div className="p-2 bg-red-100 rounded-lg">
              <AlertTriangle className="h-4 w-4 text-red-600" />
            </div>
            <span>删除确认</span>
          </AlertDialogTitle>
          <AlertDialogDescription className="space-y-3">
            <div className="text-base text-gray-700">
              确认删除机架 <span className="font-semibold text-gray-900">&ldquo;{rack?.rack}&rdquo;</span> 吗？
            </div>
            <div className="flex items-center space-x-2 text-sm text-gray-500 bg-gray-50 p-3 rounded-lg">
              <Server className="h-4 w-4" />
              <span>此操作将永久删除该机架配置，无法恢复</span>
            </div>
          </AlertDialogDescription>
        </AlertDialogHeader>
        
        <AlertDialogFooter>
          <Button variant="outline" onClick={onCancel}>
            取消
          </Button>
          <Button 
            variant="destructive" 
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading ? '删除中...' : '确定删除'}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

export default DeleteRackDialog