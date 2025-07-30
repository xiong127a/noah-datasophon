"use client"

import { useState } from 'react'
import { Modal, Form, Input, Button, message } from 'antd'
import { AddRackRequest } from '../../types/rack'

interface AddRackDialogProps {
  open: boolean
  onCancel: () => void
  onSuccess: () => void
  clusterId: number
}

const AddRackDialog = ({ open, onCancel, onSuccess, clusterId }: AddRackDialogProps) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const params: AddRackRequest = {
        rack: values.rack,
        clusterId: clusterId
      }

      // 这里需要替换为实际的API调用
      // const response = await fetch('/api/racks/save', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(params)
      // })
      // const res = await response.json()

      // 暂时使用模拟响应
      const res = { code: 200, message: '保存成功' }

      if (res.code === 200) {
        message.success('保存成功')
        form.resetFields()
        onSuccess()
      } else {
        message.error(res.message || '保存失败')
      }
    } catch (error) {
      message.error('保存失败')
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    form.resetFields()
    onCancel()
  }

  const validateRackName = (rule: any, value: string, callback: any) => {
    if (!value) {
      callback()
      return
    }

    // 检查是否包含中文
    const chineseRegex = /[\u4E00-\u9FA5]|[\uFE30-\uFFA0]/g
    if (chineseRegex.test(value)) {
      callback(new Error('名称中不能包含中文'))
      return
    }

    // 检查是否包含空格
    if (/\s/g.test(value)) {
      callback(new Error('名称中不能包含空格'))
      return
    }

    callback()
  }

  return (
    <Modal
      title="添加机架"
      open={open}
      onCancel={handleCancel}
      width={520}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          取消
        </Button>,
        <Button key="submit" type="primary" loading={loading} onClick={handleSubmit}>
          确认
        </Button>
      ]}
    >
      <div style={{ paddingTop: 10 }}>
        <Form
          form={form}
          labelCol={{ xs: { span: 24 }, sm: { span: 6 } }}
          wrapperCol={{ xs: { span: 24 }, sm: { span: 18 } }}
        >
          <Form.Item
            label="机架名称"
            name="rack"
            rules={[
              { required: true, message: '机架名称不能为空!' },
              { validator: validateRackName }
            ]}
          >
            <Input placeholder="请输入机架名称" />
          </Form.Item>
        </Form>
      </div>
    </Modal>
  )
}

export default AddRackDialog