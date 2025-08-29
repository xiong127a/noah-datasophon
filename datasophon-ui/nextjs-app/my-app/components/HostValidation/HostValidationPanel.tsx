'use client'

import React, { useState } from 'react'
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Tag, 
  Collapse, 
  Progress, 
  Typography, 
  Tooltip,
  Modal,
  Badge,
  Descriptions
} from 'antd'
import { 
  PlayCircleOutlined, 
  PauseCircleOutlined, 
  ReloadOutlined, 
  StopOutlined,
  ToolOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  EyeOutlined
} from '@ant-design/icons'
import { useHostValidation } from '../../hooks/useHostValidation'

const { Panel } = Collapse
const { Text, Title } = Typography

interface HostValidationPanelProps {
  clusterId: number
}

/**
 * 主机校验面板组件
 * 展示主机列表、检查项状态、操作按钮
 */
export const HostValidationPanel: React.FC<HostValidationPanelProps> = ({ clusterId }) => {
  const {
    hostStatuses,
    isConnected,
    isValidating,
    logs,
    pauseValidation,
    resumeValidation,
    stopValidation,
    recheckItem,
    repairItem,
    batchRepair,
    connectLogSSE
  } = useHostValidation(clusterId)

  const [selectedHost, setSelectedHost] = useState<string | null>(null)
  const [logModalVisible, setLogModalVisible] = useState(false)

  // 状态颜色映射
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'SUCCESS': return 'green'
      case 'FAILED': return 'red'
      case 'CHECKING': return 'blue'
      case 'REPAIRING': return 'orange'
      case 'PENDING': return 'default'
      default: return 'default'
    }
  }

  // 状态图标映射
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SUCCESS': return <CheckCircleOutlined />
      case 'FAILED': return <ExclamationCircleOutlined />
      case 'CHECKING': return <SyncOutlined spin />
      case 'REPAIRING': return <ToolOutlined />
      case 'PENDING': return <ClockCircleOutlined />
      default: return null
    }
  }

  // 计算整体进度
  const getOverallProgress = () => {
    if (hostStatuses.length === 0) return 0
    
    const totalItems = hostStatuses.reduce((sum, host) => sum + host.checkItems.length, 0)
    const completedItems = hostStatuses.reduce((sum, host) => 
      sum + host.checkItems.filter(item => 
        item.status === 'SUCCESS' || item.status === 'FAILED'
      ).length, 0
    )
    
    return totalItems > 0 ? Math.round((completedItems / totalItems) * 100) : 0
  }

  // 显示日志弹窗
  const showLogModal = (hostIp: string) => {
    setSelectedHost(hostIp)
    setLogModalVisible(true)
    connectLogSSE(hostIp)
  }

  // 检查项表格列定义
  const checkItemColumns = [
    {
      title: '检查项',
      dataIndex: 'displayName',
      key: 'displayName',
      width: 200,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: string) => (
        <Tag color={getStatusColor(status)} icon={getStatusIcon(status)}>
          {status}
        </Tag>
      )
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 160,
      render: (time: string) => new Date(time).toLocaleString()
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_, record: any, index: number) => {
        const hostStatus = hostStatuses.find(h => h.checkItems.includes(record))
        if (!hostStatus) return null

        return (
          <Space>
            <Tooltip title="重新检查">
              <Button
                size="small"
                icon={<ReloadOutlined />}
                onClick={() => recheckItem(hostStatus.hostIp, record.checkType)}
                disabled={record.status === 'CHECKING'}
              />
            </Tooltip>
            {record.repairAvailable && record.status === 'FAILED' && (
              <Tooltip title="修复">
                <Button
                  size="small"
                  type="primary"
                  danger
                  icon={<ToolOutlined />}
                  onClick={() => repairItem(hostStatus.hostIp, record.checkType)}
                  disabled={record.status === 'REPAIRING'}
                />
              </Tooltip>
            )}
            <Tooltip title="查看日志">
              <Button
                size="small"
                icon={<EyeOutlined />}
                onClick={() => showLogModal(hostStatus.hostIp)}
              />
            </Tooltip>
          </Space>
        )
      }
    }
  ]

  return (
    <div className="host-validation-panel">
      {/* 头部控制区 */}
      <Card className="mb-4">
        <div className="flex justify-between items-center">
          <div>
            <Title level={4} className="mb-2">主机校验控制台</Title>
            <Space>
              <Badge 
                status={isConnected ? 'success' : 'error'} 
                text={isConnected ? '已连接' : '未连接'} 
              />
              <Text type="secondary">
                总计 {hostStatuses.length} 台主机
              </Text>
            </Space>
          </div>
          
          <Space>
            <Button
              icon={<PauseCircleOutlined />}
              onClick={() => pauseValidation()}
              disabled={!isValidating}
            >
              暂停全部
            </Button>
            <Button
              icon={<PlayCircleOutlined />}
              onClick={() => resumeValidation()}
              disabled={!isValidating}
            >
              继续全部
            </Button>
            <Button
              icon={<StopOutlined />}
              danger
              onClick={() => stopValidation()}
              disabled={!isValidating}
            >
              停止全部
            </Button>
            <Button
              type="primary"
              icon={<ToolOutlined />}
              onClick={() => {
                const failedHosts = hostStatuses
                  .filter(host => host.canRepair)
                  .map(host => host.hostIp)
                if (failedHosts.length > 0) {
                  batchRepair(failedHosts)
                }
              }}
            >
              批量修复
            </Button>
          </Space>
        </div>

        {/* 整体进度 */}
        <div className="mt-4">
          <Progress 
            percent={getOverallProgress()} 
            status={isValidating ? 'active' : 'normal'}
            format={(percent) => `${percent}% 完成`}
          />
        </div>
      </Card>

      {/* 主机列表 */}
      <Card title="主机检查详情">
        <Collapse accordion>
          {hostStatuses.map((host) => (
            <Panel
              key={host.hostIp}
              header={
                <div className="flex justify-between items-center w-full">
                  <div className="flex items-center space-x-4">
                    <Badge 
                      status={getStatusColor(host.overallStatus) as any}
                      text={
                        <span className="font-medium">
                          {host.hostname || host.hostIp}
                          {host.hostname && (
                            <Text type="secondary" className="ml-2">
                              ({host.hostIp})
                            </Text>
                          )}
                        </span>
                      }
                    />
                    {host.paused && <Tag color="warning">已暂停</Tag>}
                    {host.cancelled && <Tag color="error">已取消</Tag>}
                  </div>
                  
                  <Space onClick={(e) => e.stopPropagation()}>
                    <Button
                      size="small"
                      icon={host.paused ? <PlayCircleOutlined /> : <PauseCircleOutlined />}
                      onClick={() => 
                        host.paused 
                          ? resumeValidation(host.hostIp)
                          : pauseValidation(host.hostIp)
                      }
                    >
                      {host.paused ? '继续' : '暂停'}
                    </Button>
                    <Button
                      size="small"
                      icon={<EyeOutlined />}
                      onClick={() => showLogModal(host.hostIp)}
                    >
                      日志
                    </Button>
                    <Button
                      size="small"
                      danger
                      icon={<StopOutlined />}
                      onClick={() => stopValidation(host.hostIp)}
                    >
                      停止
                    </Button>
                  </Space>
                </div>
              }
            >
              {/* 主机基本信息 */}
              <Descriptions size="small" column={3} className="mb-4">
                <Descriptions.Item label="主机IP">{host.hostIp}</Descriptions.Item>
                <Descriptions.Item label="主机名">{host.hostname || '未获取'}</Descriptions.Item>
                <Descriptions.Item label="整体状态">
                  <Tag color={getStatusColor(host.overallStatus)} icon={getStatusIcon(host.overallStatus)}>
                    {host.overallStatus}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="最后更新">
                  {new Date(host.lastUpdateTime).toLocaleString()}
                </Descriptions.Item>
                <Descriptions.Item label="可修复">
                  {host.canRepair ? '是' : '否'}
                </Descriptions.Item>
                <Descriptions.Item label="检查项数量">
                  {host.checkItems.length}
                </Descriptions.Item>
              </Descriptions>

              {/* 检查项列表 */}
              <Table
                dataSource={host.checkItems}
                columns={checkItemColumns}
                rowKey="checkType"
                size="small"
                pagination={false}
                scroll={{ x: 800 }}
              />
            </Panel>
          ))}
        </Collapse>
      </Card>

      {/* 日志弹窗 */}
      <Modal
        title={`主机日志 - ${selectedHost}`}
        open={logModalVisible}
        onCancel={() => setLogModalVisible(false)}
        footer={null}
        width={800}
        className="log-modal"
      >
        <div className="bg-black text-green-400 p-4 h-96 overflow-y-auto font-mono text-sm">
          {logs
            .filter(log => !selectedHost || log.hostIp === selectedHost)
            .map((log, index) => (
              <div key={index} className="mb-1">
                <span className="text-gray-500">{log.timestamp}</span>
                <span className={`ml-2 ${
                  log.logLevel === 'ERROR' ? 'text-red-400' :
                  log.logLevel === 'WARN' ? 'text-yellow-400' :
                  log.logLevel === 'DEBUG' ? 'text-blue-400' :
                  'text-green-400'
                }`}>
                  [{log.logLevel}]
                </span>
                <span className="ml-2 text-cyan-400">[{log.source}]</span>
                <span className="ml-2">{log.message}</span>
              </div>
            ))}
        </div>
      </Modal>
    </div>
  )
}
