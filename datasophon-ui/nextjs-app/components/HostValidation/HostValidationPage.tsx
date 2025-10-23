'use client'

import React, { useState } from 'react'
import { 
  Card, 
  Steps, 
  Button, 
  Space,
  Typography,
  Divider 
} from 'antd'
import { 
  SettingOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined 
} from '@ant-design/icons'
import { HostValidationForm } from './HostValidationForm'
import { HostValidationPanel } from './HostValidationPanel'

const { Title, Paragraph } = Typography

interface HostValidationPageProps {
  clusterId: number
}

/**
 * 主机校验主页面
 * 包含配置表单和校验结果展示
 */
export const HostValidationPage: React.FC<HostValidationPageProps> = ({ clusterId }) => {
  const [currentStep, setCurrentStep] = useState(0)
  const [validationStarted, setValidationStarted] = useState(false)

  // 步骤定义
  const steps = [
    {
      title: '配置校验',
      description: '设置SSH连接信息',
      icon: <SettingOutlined />
    },
    {
      title: '执行校验',
      description: '实时查看校验进度',
      icon: <PlayCircleOutlined />
    },
    {
      title: '查看结果',
      description: '检查结果并修复问题',
      icon: <CheckCircleOutlined />
    }
  ]

  // 处理校验启动
  const handleValidationStart = () => {
    setValidationStarted(true)
    setCurrentStep(1)
  }

  // 重新配置
  const handleReconfigure = () => {
    setValidationStarted(false)
    setCurrentStep(0)
  }

  return (
    <div className="host-validation-page p-6">
      {/* 页面头部 */}
      <Card className="mb-6">
        <Title level={2} className="mb-2">
          主机校验
        </Title>
        <Paragraph className="text-gray-600 mb-4">
          对集群主机进行全面的环境检查，包括SSH连接、系统信息、网络配置等多个维度的校验，
          确保主机环境满足大数据组件部署要求。
        </Paragraph>
        
        {/* 步骤指示器 */}
        <Steps
          current={currentStep}
          items={steps}
          className="mb-4"
        />

        {validationStarted && (
          <div className="text-center">
            <Space>
              <Button 
                onClick={handleReconfigure}
                disabled={currentStep === 1}
              >
                重新配置
              </Button>
              <Button 
                type="primary"
                onClick={() => setCurrentStep(2)}
                disabled={currentStep !== 1}
              >
                查看结果
              </Button>
            </Space>
          </div>
        )}
      </Card>

      {/* 内容区域 */}
      {currentStep === 0 && (
        <HostValidationForm 
          clusterId={clusterId}
          onValidationStart={handleValidationStart}
        />
      )}

      {(currentStep === 1 || currentStep === 2) && validationStarted && (
        <>
          <HostValidationPanel clusterId={clusterId} />
          
          {currentStep === 1 && (
            <Card className="mt-4">
              <div className="text-center">
                <Title level={4} className="mb-2">校验进行中...</Title>
                <Paragraph type="secondary">
                  请耐心等待校验完成，可以实时查看各主机的校验进度和日志信息。
                  如有问题可随时暂停或停止校验任务。
                </Paragraph>
                <Button 
                  type="primary" 
                  onClick={() => setCurrentStep(2)}
                  className="mt-2"
                >
                  切换到结果视图
                </Button>
              </div>
            </Card>
          )}

          {currentStep === 2 && (
            <Card className="mt-4">
              <div className="text-center">
                <Title level={4} className="mb-2">校验结果</Title>
                <Paragraph type="secondary">
                  校验已完成，请查看各主机的检查结果。对于失败的检查项，
                  可以点击对应的修复按钮进行自动修复。
                </Paragraph>
                <Space className="mt-2">
                  <Button onClick={() => setCurrentStep(1)}>
                    返回进度视图
                  </Button>
                  <Button 
                    type="primary" 
                    onClick={handleReconfigure}
                  >
                    重新开始校验
                  </Button>
                </Space>
              </div>
            </Card>
          )}
        </>
      )}

      {/* 帮助信息 */}
      <Card title="使用说明" className="mt-6">
        <div className="grid md:grid-cols-2 gap-6">
          <div>
            <Title level={5}>校验前准备</Title>
            <ul className="list-disc list-inside space-y-1 text-sm">
              <li>确保目标主机网络连通</li>
              <li>准备SSH登录凭据（密码或私钥）</li>
              <li>确认SSH服务正常运行</li>
              <li>检查防火墙设置允许SSH连接</li>
            </ul>
          </div>
          
          <div>
            <Title level={5}>校验内容</Title>
            <ul className="list-disc list-inside space-y-1 text-sm">
              <li>SSH连接可用性（免密优先）</li>
              <li>系统资源状态（CPU、内存、磁盘）</li>
              <li>Java运行环境检查</li>
              <li>网络配置和连通性</li>
              <li>系统安全设置（防火墙、SELinux）</li>
              <li>时间同步和系统服务状态</li>
            </ul>
          </div>
          
          <div>
            <Title level={5}>修复功能</Title>
            <ul className="list-disc list-inside space-y-1 text-sm">
              <li>自动修复常见配置问题</li>
              <li>实时查看修复过程日志</li>
              <li>支持单项和批量修复</li>
              <li>修复后自动重新校验</li>
            </ul>
          </div>
          
          <div>
            <Title level={5}>注意事项</Title>
            <ul className="list-disc list-inside space-y-1 text-sm">
              <li>校验过程中请勿关闭浏览器</li>
              <li>大量主机校验可能耗时较长</li>
              <li>修复操作会修改系统配置</li>
              <li>建议在测试环境先行验证</li>
            </ul>
          </div>
        </div>
      </Card>
    </div>
  )
}
