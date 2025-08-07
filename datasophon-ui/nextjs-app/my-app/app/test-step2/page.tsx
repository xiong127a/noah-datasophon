"use client"

import React, { useState } from 'react'
import { Button } from "@/components/ui/button"
import { ClusterStep2Dialog } from '@/components/cluster'

export default function TestStep2Page() {
  const [dialogOpen, setDialogOpen] = useState(false)

  // 模拟集群数据
  const mockCluster = {
    id: 1,
    clusterName: '测试集群',
    depType: 'Kubernetes', // 或 'PVM'
    clusterCode: 'test-cluster-001'
  }

  // 模拟step1数据
  const mockStep1Data = {
    hosts: '192.168.1.10,192.168.1.11,192.168.1.12',
    sshUser: 'root',
    sshPort: '22',
    sshPassword: 'password123',
    kubeConfigContent: 'apiVersion: v1\nkind: Config\n...',
    namespace: 'default'
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Step2 主机环境校验测试页面</h1>
        
        <div className="bg-white rounded-lg shadow p-6 mb-8">
          <h2 className="text-xl font-semibold mb-4">组件信息</h2>
          <div className="space-y-2 text-gray-600">
            <p><strong>功能：</strong>主机环境校验</p>
            <p><strong>支持模式：</strong>Kubernetes、PVM (传统虚拟机)</p>
            <p><strong>主要特性：</strong></p>
            <ul className="list-disc list-inside ml-4 space-y-1">
              <li>两种部署模式的主机校验</li>
              <li>实时状态监控和队列管理</li>
              <li>主机选择和批量操作</li>
              <li>校验失败重试机制</li>
              <li>日志查看和问题诊断</li>
            </ul>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4">测试操作</h2>
          <div className="space-y-4">
            <Button 
              onClick={() => setDialogOpen(true)}
              className="bg-blue-600 hover:bg-blue-700"
            >
              打开主机环境校验对话框
            </Button>
            
            <div className="text-sm text-gray-500">
              <p>点击按钮打开Step2对话框，测试主机环境校验功能</p>
              <p>当前模拟数据：{mockCluster.depType} 模式，集群名称：{mockCluster.clusterName}</p>
            </div>
          </div>
        </div>

        <ClusterStep2Dialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          cluster={mockCluster}
          step1Data={mockStep1Data}
          onSuccess={() => {
            console.log('Step2 完成')
            setDialogOpen(false)
          }}
          onPrevious={() => {
            console.log('返回上一步')
            setDialogOpen(false)
          }}
        />
      </div>
    </div>
  )
}