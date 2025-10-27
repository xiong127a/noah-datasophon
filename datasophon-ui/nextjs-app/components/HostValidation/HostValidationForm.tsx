'use client'

import React, { useState } from 'react'
import { 
  Form, 
  Input, 
  Button, 
  Card, 
  Space, 
  InputNumber,
  Radio,
  Upload,
  message,
  Divider,
  Alert,
  Typography
} from 'antd'
import { 
  UploadOutlined, 
  PlayCircleOutlined,
  InfoCircleOutlined 
} from '@ant-design/icons'
import { useHostValidation } from '../../hooks/useHostValidation'

const { TextArea } = Input
const { Title, Text } = Typography

interface HostValidationFormProps {
  clusterId: string
  onValidationStart?: () => void
}

/**
 * 主机校验启动表单
 * 配置SSH连接信息并启动校验
 */
export const HostValidationForm: React.FC<HostValidationFormProps> = ({ 
  clusterId, 
  onValidationStart 
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [authMethod, setAuthMethod] = useState<'password' | 'key'>('password')
  
  const { startValidation } = useHostValidation(clusterId)

  // 处理表单提交
  const handleSubmit = async (values: any) => {
    setLoading(true)
    try {
      // 解析IP列表
      const hostIps = values.hostIps
        .split('\n')
        .map((ip: string) => ip.trim())
        .filter((ip: string) => ip.length > 0)

      if (hostIps.length === 0) {
        message.error('请输入至少一个主机IP')
        return
      }

      // 构建请求参数
      const request = {
        clusterId,
        hostIps,
        sshUser: values.sshUser,
        sshPort: values.sshPort || 22,
        ...(authMethod === 'password' 
          ? { sshPassword: values.sshPassword }
          : { privateKeyPath: values.privateKeyPath }
        )
      }

      await startValidation(request)
      onValidationStart?.()
      
    } catch (error) {
      console.error('启动校验失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 私钥文件上传处理
  const handleKeyUpload = (info: any) => {
    if (info.file.status === 'done') {
      form.setFieldValue('privateKeyPath', info.file.response?.filePath)
      message.success('私钥文件上传成功')
    } else if (info.file.status === 'error') {
      message.error('私钥文件上传失败')
    }
  }

  return (
    <Card title="主机校验配置" className="mb-4">
      <Alert
        message="主机校验说明"
        description="此功能将对指定主机进行SSH连接测试、系统信息收集和环境检查。请确保SSH连接信息正确，支持密码和私钥两种认证方式。"
        type="info"
        icon={<InfoCircleOutlined />}
        className="mb-4"
        showIcon
      />

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          sshPort: 22,
          sshUser: 'root',
          authMethod: 'password'
        }}
      >
        <Form.Item
          label="主机IP列表"
          name="hostIps"
          rules={[{ required: true, message: '请输入主机IP列表' }]}
          extra="每行输入一个IP地址，支持IPv4格式"
        >
          <TextArea
            rows={6}
            placeholder={`请输入主机IP，每行一个，例如：
192.168.1.10
192.168.1.11
192.168.1.12`}
          />
        </Form.Item>

        <div className="grid grid-cols-2 gap-4">
          <Form.Item
            label="SSH用户名"
            name="sshUser"
            rules={[{ required: true, message: '请输入SSH用户名' }]}
          >
            <Input placeholder="默认: root" />
          </Form.Item>

          <Form.Item
            label="SSH端口"
            name="sshPort"
            rules={[{ required: true, message: '请输入SSH端口' }]}
          >
            <InputNumber
              min={1}
              max={65535}
              placeholder="默认: 22"
              className="w-full"
            />
          </Form.Item>
        </div>

        <Divider>认证方式</Divider>

        <Form.Item
          label="认证方式"
          name="authMethod"
          rules={[{ required: true }]}
        >
          <Radio.Group 
            onChange={(e) => setAuthMethod(e.target.value)}
            value={authMethod}
          >
            <Radio value="password">密码认证</Radio>
            <Radio value="key">私钥认证（免密）</Radio>
          </Radio.Group>
        </Form.Item>

        {authMethod === 'password' && (
          <Form.Item
            label="SSH密码"
            name="sshPassword"
            rules={[{ required: true, message: '请输入SSH密码' }]}
          >
            <Input.Password placeholder="请输入SSH登录密码" />
          </Form.Item>
        )}

        {authMethod === 'key' && (
          <Form.Item
            label="私钥文件路径"
            name="privateKeyPath"
            rules={[{ required: true, message: '请输入私钥文件路径或上传私钥文件' }]}
            extra="可以直接输入服务器上的私钥文件路径，如 /root/.ssh/id_rsa"
          >
            <Space.Compact className="w-full">
              <Input 
                placeholder="私钥文件路径，如: /root/.ssh/id_rsa" 
                className="flex-1"
              />
              <Upload
                name="keyFile"
                action="/ddh/api/v1/upload/ssh-key"
                onChange={handleKeyUpload}
                showUploadList={false}
                accept=".pem,.key,.rsa"
              >
                <Button icon={<UploadOutlined />}>上传</Button>
              </Upload>
            </Space.Compact>
          </Form.Item>
        )}

        <Divider />

        <div className="text-center">
          <Space>
            <Button
              type="primary"
              size="large"
              icon={<PlayCircleOutlined />}
              htmlType="submit"
              loading={loading}
            >
              开始校验
            </Button>
            <Button 
              size="large"
              onClick={() => form.resetFields()}
            >
              重置
            </Button>
          </Space>
        </div>
      </Form>

      <Divider />

      <div className="bg-gray-50 p-4 rounded">
        <Title level={5} className="mb-2">校验内容包括：</Title>
        <div className="grid grid-cols-2 gap-2 text-sm">
          <div>✓ SSH免密连接检查</div>
          <div>✓ SSH密码连接检查</div>
          <div>✓ 系统信息收集</div>
          <div>✓ CPU、内存、磁盘检查</div>
          <div>✓ Java环境检查</div>
          <div>✓ 网络连通性检查</div>
          <div>✓ 防火墙状态检查</div>
          <div>✓ SELinux状态检查</div>
          <div>✓ 系统服务检查</div>
          <div>✓ Hosts文件检查</div>
          <div>✓ 文件句柄限制检查</div>
          <div>✓ 时间同步检查</div>
        </div>
        
        <Alert
          message="校验完成后，失败的检查项可以通过修复功能自动修复"
          type="success"
          className="mt-3"
          showIcon
        />
      </div>
    </Card>
  )
}
