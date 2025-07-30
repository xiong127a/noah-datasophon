"use client"

import { useState } from 'react'
import { Modal, Button, message } from 'antd'
import { QuestionCircleOutlined } from '@ant-design/icons'
import { Rack, DeleteRackRequest } from '../../types/rack'

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

      const params: DeleteRackRequest = {
        rackId: rack.id
      }

      // 这里需要替换为实际的API调用
      // const response = await fetch('/api/racks/delete', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(params)
      // })
      // const res = await response.json()

      // 暂时使用模拟响应
      const res = { code: 200, message: '删除成功' }

      if (res.code === 200) {
        message.success('删除成功')
        onSuccess()
      } else {
        message.error(res.message || '删除失败')
      }
    } catch (error) {
      message.error('删除失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      title={
        <div className="flex items-center">
          <QuestionCircleOutlined className="text-blue-500 mr-2" />
          提示
        </div>
      }
      open={open}
      onCancel={onCancel}
      width={400}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          取消
        </Button>,
        <Button key="submit" type="primary" loading={loading} onClick={handleSubmit}>
          确定
        </Button>
      ]}
    >
      <div style={{ padding: '16px 0' }}>
        <span>确认删除当前机架？</span>
      </div>
    </Modal>
  )
}

export default DeleteRackDialog