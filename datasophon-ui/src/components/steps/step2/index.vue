<!--
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/* eslint-disable vue/no-unused-components */

 * @describe: step2-主机环境校验 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-08-15 14:07:04
 * @FilePath: \ddh-ui\src\components\steps\index.vue
-->
<template>
  <div class="steps2 steps">
    <!-- 添加OsFloatingCard组件的使用 -->
    <os-floating-card v-if="false"></os-floating-card>
    
    <div class="hero-section">
      <h1 class="hero-title">主机环境校验</h1>
      <p class="hero-subtitle">验证主机环境配置，确保系统顺利部署</p>

      <div class="queue-status-area">
        <queue-status-indicator :queue-status="queueStatus" />
      </div>
    </div>

    <div class="hosts-table-container">
      <!-- 添加表格操作按钮区域 -->
      <div class="table-operations" style="margin-bottom: 16px; display: flex; justify-content: flex-end; align-items: center;">
        <div class="operation-group">
          <!-- 开始检查/重试/终止检查三合一按钮 -->
          <a-button
              class="apple-button"
              :class="isCheckingActive ? 'apple-danger-button' : 'apple-primary-button'"
              @click="handleCheckAction"
          >
            <a-icon :type="isCheckingActive ? 'stop' : (hasStartedCheck ? 'redo' : 'play-circle')" />
            <span>{{ isCheckingActive ? '终止检查' : (hasStartedCheck ? '重试检查' : '开始检查') }}</span>
          </a-button>
        </div>
      </div>
      <a-table
          @change="tableChange"
          :columns="columns"
          :loading="loading"
          :dataSource="dataSource"
          :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
          rowKey="ip"
          :pagination="pagination"
          expandable
          :expandedRowRender="expandedRowRender"
          :expandIcon="customExpandIcon"
          :class="'host-check-table'"
      >
      </a-table>
    </div>

    <!-- 使用新的AppleLogViewer组件 -->
    <apple-log-viewer
      :clusterId="clusterId"
      :visible.sync="logVisible"
      :title="logModalTitle"
      :hostIp="currentLogIp"
      :itemId="currentLogItemId"
      :itemName="currentLogItemName"
      @close="closeLogModal"
    />

    <!-- 修复确认弹窗 - 苹果设计风格 -->
    <a-modal
        v-model="fixConfirmVisible"
        :footer="null"
        :maskClosable="false"
        width="460px"
        :destroyOnClose="true"
        :closable="false"
        class="fix-confirm-modal apple-card-modal"
    >
      <!-- 弹窗内容 -->
      <div class="apple-card-container">
        <!-- 顶部标题区域 -->
        <div class="apple-card-header">
          <div class="apple-card-icon-wrapper">
            <div class="apple-card-icon-container warning">
              <svg viewBox="0 0 24 24" width="20" height="20" stroke="#FFFFFF" fill="none" stroke-width="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                <line x1="12" y1="9" x2="12" y2="13" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
            </div>
          </div>
          <div class="apple-card-info">
            <div class="apple-card-title">
              <span class="card-title">{{ fixConfirmTitle }}</span>
            </div>
          </div>
          <div class="apple-card-close" @click="handleFixCancel">
            <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none">
              <circle cx="12" cy="12" r="10" />
              <line x1="15" y1="9" x2="9" y2="15" />
              <line x1="9" y1="9" x2="15" y2="15" />
            </svg>
          </div>
        </div>
        
        <!-- 内容区域 -->
        <div class="apple-card-content">
          <div class="apple-card-section">
            <div class="warning-message">
              <div class="message-content" v-html="fixConfirmContent"></div>
            </div>
          </div>
        </div>
        
        <!-- 底部按钮区域 -->
        <div class="apple-card-footer">
          <button class="apple-card-button secondary" @click="handleFixCancel">
            取消
          </button>
          <button 
            class="apple-card-button danger" 
            @click="handleFixConfirm"
            :disabled="fixConfirmLoading"
          >
            <span v-if="!fixConfirmLoading">确认修复</span>
            <span v-else class="apple-loader"></span>
          </button>
        </div>
      </div>
    </a-modal>

    <!-- 添加主机名编辑对话框 -->
    <a-modal
      :visible="hostnameEditVisible"
      :confirm-loading="editLoading"
      @cancel="cancelHostnameEdit"
      :footer="null"
      :maskClosable="false"
      :closable="false"
      width="420px"
      class="apple-style-modal hostname-edit-modal"
      :destroyOnClose="true"
    >
      <div class="apple-modal-header">
        <div class="modal-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="#007AFF" stroke-width="1.5" width="20" height="20">
            <path d="M12 15V12m0-3h.01M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0z" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="modal-title">修改主机名</div>
        <div class="modal-close" @click="cancelHostnameEdit">
          <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="1.5" fill="none">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </div>
      </div>
      
      <div class="apple-modal-content">
        <div class="host-info-section">
          <div class="info-row">
            <div class="info-label">主机IP</div>
            <div class="info-value">{{ currentEditHost ? currentEditHost.ip : '' }}</div>
          </div>
          <div class="info-row">
            <div class="info-label">当前主机名</div>
            <div class="info-value current-hostname">{{ currentEditHost ? (currentEditHost.hostname || '未设置') : '' }}</div>
          </div>
        </div>
        
        <div class="input-section">
          <div class="input-label">新主机名</div>
          <input
            class="apple-input"
            v-model="newHostname"
            placeholder="请输入新的主机名"
            maxlength="64"
            ref="hostnameInput"
          />
          <div class="input-description">
            <svg viewBox="0 0 24 24" fill="none" stroke="#8E8E93" stroke-width="1.5" width="16" height="16">
              <circle cx="12" cy="12" r="10"></circle>
              <path d="M12 16v-4M12 8h.01" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>修改主机名将通过SSH连接服务器并实际修改系统配置</span>
          </div>
        </div>
        
        <div class="apple-modal-actions">
          <button class="apple-button secondary" @click="cancelHostnameEdit">
            取消
          </button>
          <button class="apple-button primary" @click="submitHostnameEdit" :disabled="!newHostname">
            <span v-if="!editLoading">保存</span>
            <span v-else class="button-loader"></span>
          </button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import LogFilter from './LogFilter.vue';
import QueueStatusIndicator from '@/components/steps/step2/QueueStatusIndicator.vue'
// 导入操作系统浮窗组件
import OsFloatingCard from '@/components/steps/step2/OsFloatingCard.vue';
// 导入HostnameFloatingCard组件
import HostnameFloatingCard from '@/components/steps/step2/HostnameFloatingCard.vue';
import AppleLogViewer from './AppleLogViewer.vue';

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps1Data: Object,
    depType:String,
  },
  components: {
    LogFilter,
    QueueStatusIndicator,
    OsFloatingCard,
    /* eslint-disable-next-line vue/no-unused-components */
    HostnameFloatingCard,
    AppleLogViewer
  },
  data() {
    return {
      loading: false,
      isRequesting: false,
      isCheckingActive: false, // 是否有检查正在进行中
      hasStartedCheck: false, // 是否已开始过检查
      dataSource: [],
      selectedRowKeys: [],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "50", "100", "500", "1000"],
        showTotal: (total) => `共 ${total} 条`,
      },
      timer: null,
      firstDataLoaded: false,
      checkItemsMap: {}, // 存储每个主机的校验项
      checkItem: null, // 当前查看日志的检查项
      // 使用新的AppleLogViewer组件，不再需要日志相关变量
      queueStatus: {
        queueSize: 0,
        runningTasks: 0,
        processorThreadAlive: true
      }, // 队列状态信息
      // 添加确认弹窗相关状态
      fixConfirmVisible: false,
      fixConfirmLoading: false,
      fixConfirmTitle: '',
      fixConfirmContent: '',
      fixConfirmIp: '',
      fixConfirmItem: null,
      columns: [
        {
          title: "序号",
          key: "index",
          width: 50,
          customRender: (text, row, index) => {
            const h = this.$createElement;
            const displayIndex = parseInt(
                this.pagination.current === 1
                    ? index + 1
                    : index + 1 + this.pagination.pageSize * (this.pagination.current - 1)
            );
            return h('span', {}, [displayIndex]);
          },
        },
        {
          title: "主机名",
          key: "hostname",
          dataIndex: "hostname",
          width: 200,
          customRender: (text, record) => {
            const h = this.$createElement;
            return h('div', { class: 'hostname-column' }, [
              // 添加主机名悬浮卡片
              h('a-tooltip', {
                props: {
                  placement: 'right',
                  arrowPointAtCenter: true,
                  overlayClassName: 'hostname-tooltip',
                  getPopupContainer: () => document.body
                }
              }, [
                // 使用主机名浮窗组件
                h('span', {
                  slot: 'title',
                  class: 'hostname-detail-tooltip'
                }, [
                  h(HostnameFloatingCard, {
                    props: {
                      hostInfo: record
                    }
                  })
                ]),
                
                // 显示的主机名文本
                h('span', {
                  class: 'hostname-text', 
                  title: record.fqdn || record.hostname || '未知主机名' 
                }, [record.hostname || '未知主机名']),
              ]),
              
              // 编辑图标
              h('a-tooltip', { props: { title: '编辑主机名' } }, [
                h('a-icon', {
                  class: 'hostname-edit-icon',
                  props: { type: 'edit' },
                  on: {
                    click: (e) => {
                      e.stopPropagation();
                      this.editHostname(record);
                    }
                  }
                })
              ])
            ]);
          }
        },
        {
          title: "主机IP",
          key: "ip",
          dataIndex: "ip",
          width: 130,
          customRender: (text) => {
            const h = this.$createElement;
            return h('span', {
              style: {
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis'
              }
            }, [text]);
          }
        },
        {
          title: "操作系统",
          key: "osType",
          dataIndex: "osType",
          width: "10%",  // 减小操作系统列宽度，从15%改为10%
          customRender: (text, row) => {
            const h = this.$createElement;

            // 如果SSH连接失败或未初始化或正在加载，则显示加载中状态
            if (row.sshConnectStatus === null || 
                row.osInfo === null || row.osInfoStatus === null ||
                this.checkStatus(row.osStatus, 'loading') ||
                this.checkStatus(row.osStatus, 'pending')) {
              // 如果SSH连接失败，则显示错误状态而不是加载状态
              if (this.checkStatus(row.sshConnectStatus, 'error') || row.hasSSHError === true) {
                return h('div', {
                  style: {
                    display: 'flex',
                    alignItems: 'center',
                    color: '#FF3B30'
                  }
                }, [
                  h('a-icon', {
                    props: { type: 'warning' },
                    style: { marginRight: '6px' }
                  }),
                  '无法连接SSH'
                ]);
              }

              // 苹果风格的骨架屏加载动画
              return h('a-tooltip', {
                props: {
                  placement: 'right',
                  arrowPointAtCenter: true,
                  overlayClassName: 'os-tooltip',
                  getPopupContainer: () => document.body
                }
              }, [
                // 加载中浮窗内容
                h('span', {
                  slot: 'title',
                  class: 'os-detail-tooltip'
                }, [
                  h('div', { class: 'os-detail-loading' }, [
                    h('div', { class: 'os-detail-loading-header' }),
                    h('div', { class: 'os-detail-loading-content' }, [
                      h('div', { class: 'os-detail-loading-line short' }),
                      h('div', { class: 'os-detail-loading-line medium' }),
                      h('div', { class: 'os-detail-loading-line' }),
                      h('div', { class: 'os-detail-loading-line short' }),
                      h('div', { class: 'os-detail-loading-line medium' })
                    ]),
                    h('div', {
                      class: 'os-detail-loading-text',
                      style: {
                        fontSize: '14px',
                        textAlign: 'center',
                        color: '#007AFF',
                        marginTop: '12px',
                        fontWeight: '500'
                      }
                    }, ['正在优雅地检索操作系统信息...'])
                  ])
                ]),

                // 显示的加载内容
                h('div', {
                  class: 'os-info-loading',
                  style: {
                    position: 'relative',
                    height: '24px',
                    width: '100%',
                    borderRadius: '4px',
                    backgroundColor: 'rgba(240, 240, 240, 0.8)',
                    overflow: 'hidden',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }
                }, [
                  // 添加加载中文字
                  h('span', {
                    style: {
                      fontSize: '12px',
                      color: '#8E8E93',
                      fontWeight: '500',
                      position: 'relative',
                      zIndex: 2,
                      whiteSpace: 'nowrap'  // 防止文字换行
                    }
                  }, ['正在检索操作系统...'])
                ])
              ]);
            }

            // 添加处理操作系统信息显示错误的情况
            if (this.checkStatus(row.osInfoStatus, 'error') ||
                this.checkStatus(row.osStatus, 'error')) {
              return h('div', {
                style: {
                  display: 'flex',
                  alignItems: 'center',
                  color: '#FF3B30'
                }
              }, [
                h('a-icon', {
                  props: { type: 'warning' },
                  style: { marginRight: '6px' }
                }),
                '操作系统信息获取失败'
              ]);
            }

            // 使用osInfo中的数据
            const hasOsInfo = row.osInfo && (row.osInfo.distribution || row.osInfo.displayName);
            
            // 只使用displayName字段，不使用distributionName和distribution
            const osDisplayName = hasOsInfo 
              ? (row.osInfo.displayName || row.osInfo.distribution || '-')
              : (text || row.osType || '-');
              
            const osVersion = hasOsInfo ? row.osInfo.versionId : (row.osVersion || '');

            // 获取操作系统对应的图标路径
            function getOsIconPath(osInfo) {
              try {
                if (!osInfo) return require('@/assets/img/os-logos/linux-tux.svg');
                
                // 根据osInfo.distributionType或distributionId判断操作系统类型
                const distType = (osInfo.distributionType || '').toLowerCase();
                const distId = (osInfo.distributionId || '').toLowerCase();
                const distName = (osInfo.distribution || '').toLowerCase();
                
                // 确定主操作系统类型
                let osType = 'linux';
                
                if (distType === 'centos' || distId === 'centos' || distName.includes('centos')) {
                  osType = 'centos';
                } else if (distType === 'ubuntu' || distId === 'ubuntu' || distName.includes('ubuntu')) {
                  osType = 'ubuntu';
                } else if (distType === 'debian' || distId === 'debian' || distName.includes('debian')) {
                  osType = 'debian';
                } else if (distType === 'redhat' || distId === 'redhat' || distName.includes('redhat') || distName.includes('red hat')) {
                  osType = 'redhat';
                } else if (distType === 'windows' || distId === 'windows' || distName.includes('windows')) {
                  osType = 'windows';
                } else if (distType === 'kylin' || distId === 'kylin' || distName.includes('kylin') || distName.includes('麒麟')) {
                  osType = 'kylin';
                }
                
                // 使用switch语句根据操作系统类型返回对应图标
                switch (osType) {
                  case 'centos':
                    return require('@/assets/img/os-logos/centos.svg');
                  case 'ubuntu':
                    return require('@/assets/img/os-logos/ubuntu.svg');
                  case 'debian':
                    return require('@/assets/img/os-logos/debian.svg');
                  case 'redhat':
                    return require('@/assets/img/os-logos/redhat.svg');
                  case 'windows':
                    return require('@/assets/img/os-logos/windows.svg');
                  case 'kylin':
                    return require('@/assets/img/os-logos/kylin.svg');
                  default:
                    return require('@/assets/img/os-logos/linux-tux.svg');
                }
              } catch (error) {
                // 如果找不到图标文件，返回内置的数据URI
                return 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA0OCA0OCIgZmlsbD0ibm9uZSI+PHJlY3Qgd2lkdGg9IjQ4IiBoZWlnaHQ9IjQ4IiByeD0iOCIgZmlsbD0iI2YwZjBmMCIvPjxwYXRoIGQ9Ik0yMy41IDE0QzIzLjUgMTIuMzQzMSAyNC44NDMxIDExIDI2LjUgMTFDMjguMTU2OSAxMSAyOS41IDEyLjM0MzEgMjkuNSAxNFYxNy42NzY4QzMwLjQ5MzcgMTguMTA3MiAzMS4zNjc0IDE4Ljc4NTUgMzIgMTkuNjMyVjE0QzMyIDExLjIzODYgMjkuNzYxNCA5IDI3IDlDMjQuMjM4NiA5IDIyIDExLjIzODYgMjIgMTRWMTkuNjM0QzIyLjYzMzEgMTguNzg2MSAyMy41MDc0IDE4LjEwNzEgMjQuNSAxNy42NzZWMTRIMjMuNVoiIGZpbGw9IiM1MjUyNTIiLz48cGF0aCBkPSJNMzEuOTk5OCAyOC45OUMzMi4wMDE4IDI5LjYzODkgMzEuODA3MSAzMC4yNzMzIDMxLjQ0MjkgMzAuODAyQzMxLjA3ODYgMzEuMzMwNyAzMC41NjAyIDMxLjczMDUgMjkuOTU5OCAzMS45NVYzNC43MkMzMi45MDc1IDM0LjEyMTMgMzUuMTAyIDMxLjM5NjYgMzUgMjguMjlDMzQuODk3OSAyNS4xODM0IDMyLjU1OTYgMjIuNjM5MiAyOS41IDIyLjI1VjE5LjI4QzI5LjUgMTkuMjggMzggMjEuMjggMzggMjlDMzggMzYuNzIgMjkuNTUgMzggMjkuNTUgMzhIMTkuMDNDMTkuMDMgMzggMTAuNTIgMzcuMjkgMTAuMDIgMjcuNzhDOS42OCAxOS43OSAxOS41IDE4LjI3IDE5LjUgMTguMjdWMjEuMjdDMTkuNSAyMS4yNyAxMy4wMDk4IDIyLjYxIDE0LjAyIDE5QzE1LjUgMTQgMjQuOTk5OCAxNCAyNC45OTk4IDE0QzI0Ljk5OTggMTQgMjYuOTk5OCAxNCAyOS4wMDA3IDE0Ljk5QzI5LjAwMDcgMTQuOTkgMjguOTUxNCAxNi42OTMxIDI4LjAyMDcgMTcuODJDMjUuNjgwNyAxOC40OSAyMyAyMC41MSAyMyAyNC41QzIzIDI5LjE1IDI3LjAwMDIgMzAuMTcgMjcuMDAwMiAzMS4yNVYzNC42NkMyMi42NDczIDM0LjMzMDMgMTkuMTk5MSAzMC42NjAzIDE5LjAxOTggMjYuMDZDMTkuMDE5OCAyNS44NiAxOS4wMTk4IDI1LjY2IDE5LjAxOTggMjUuNDZDMTkuMDE5OCAyMy42OTQ1IDE5LjYzOTQgMjEuOTkxMiAyMC43Mzk3IDIwLjY4MTdDMjEuODQwMSAxOS4zNzIyIDIzLjM0NDIgMTguNTUxNiAyNC45OTk4IDE4LjQyVjIyLjE5QzIzLjI4MTQgMjIuNDA1NiAyMS44NTA1IDIzLjU0MTMgMjEuMzcwOSAyNS4xN0MyMi4yMTc3IDI3LjY0MjcgMjQuNzY1OCAyOS4xMjI0IDI3LjI3MDcgMjguNThDMjcuNzk5OSAyOC40NiAyOC4zMTYyIDI4LjI5MTQgMjguODE4MyAyOC4wOEMyOS4wNTQ5IDI3Ljk4ODYgMjkuMzE2MyAyNy45OTk3IDI5LjU0OCAyOC4xMTFDMjkuNzc5NyAyOC4yMjIzIDI5Ljk2MDIgMjguNDI1MiAzMC4wNCAyOC42OEMzMC4yMSAyOS4xNTcgMzAuMzI0NCAyOS42NTMzIDMwLjM4MTcgMzAuMTU3M0MzMC41MDUzIDI5LjY3MjggMzAuNTA4NSAyOS4xNTgxIDMwLjM5MDkgMjguNjcyQzMwLjM5MDkgMjguNjcyIDMxLjk5OTggMjguOTkgMzEuOTk5OCAyOC45OVoiIGZpbGw9IiM1MjUyNTIiLz48L3N2Zz4=';
              }
            }

            const iconPath = getOsIconPath(hasOsInfo ? row.osInfo : null);

            // 返回操作系统信息显示
            if (hasOsInfo) {
              return h('a-tooltip', {
                props: {
                  placement: 'right',
                  arrowPointAtCenter: true,
                  overlayClassName: 'os-tooltip',
                  getPopupContainer: () => document.body
                }
              }, [
                // 使用新的操作系统浮窗组件
                h('span', {
                  slot: 'title',
                  class: 'os-detail-tooltip'
                }, [
                  h(OsFloatingCard, {
                    props: {
                      osInfo: row.osInfo,
                      cpuStatus: row.cpuStatus || 'pending',
                      memoryStatus: row.memoryStatus || 'pending',
                      diskStatus: row.diskStatus || 'pending',
                      swapStatus: row.swapStatus || 'pending',
                      gpuStatus: row.gpuStatus || 'pending'
                    }
                  })
                ]),

                // 显示的操作系统信息
                h('div', {
                  style: {
                    display: 'flex',
                    alignItems: 'center'
                  }
                }, [
                  // 操作系统图标
                  h('div', {
                    style: {
                      width: '24px',
                      height: '24px',
                      marginRight: '8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }
                  }, [
                    h('img', {
                      attrs: {
                        src: iconPath,
                        alt: osDisplayName
                      },
                      style: {
                        width: '20px',
                        height: '20px'
                      }
                    })
                  ]),

                  // 操作系统名称和版本
                  h('div', {
                    style: {
                      display: 'flex',
                      flexDirection: 'column'
                    }
                  }, [
                    h('span', {
                      style: {
                        color: '#1D1D1F',
                        fontWeight: '500',
                        fontSize: '13px',
                        lineHeight: '1.3'
                      }
                    }, [osDisplayName]),
                    osVersion ? h('span', {
                      style: {
                        color: '#8E8E93',
                        fontSize: '11px',
                        lineHeight: '1.3'
                      }
                    }, [osVersion]) : null
                  ])
                ])
              ]);
            }

            // 当没有有效的osInfo时，显示简单的信息
            return h('div', {
              style: {
                display: 'flex',
                alignItems: 'center'
              }
            }, [
              h('span', {
                style: {
                  color: '#8E8E93',
                  fontSize: '13px'
                }
              }, [text || '未知操作系统'])
            ]);
          }
        },
        {
          title: "当前受管",
          key: "managed",
          dataIndex: "managed",
          width: "90px", // 添加固定宽度
          customRender: (text, row, index) => {
            const h = this.$createElement;
            return h('div', {
              style: {
                display: 'inline-flex',
                alignItems: 'center',
                padding: '4px 12px',
                borderRadius: '12px',
                fontSize: '13px',
                fontWeight: '500',
                backgroundColor: text ? 'rgba(52, 199, 89, 0.1)' : 'rgba(142, 142, 147, 0.1)',
                color: text ? '#34c759' : '#8e8e93',
                transition: 'all 0.3s ease'
              }
            }, [
              h('span', {
                style: {
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: text ? '#34c759' : '#8e8e93',
                  marginRight: '6px',
                  display: 'inline-block'
                }
              }),
              text ? "是" : "否"
            ]);
          },
        },
        {
          title: "状态",
          key: "status",
          width: "15%",  // 增加状态列宽度
          customRender: (text, row) => {
            const h = this.$createElement;

            // 状态映射
            const statusMap = {
              CHECKING: { text: '检查中', color: '#1890ff', icon: 'loading' },
              WAITING: { text: '等待检查', color: '#faad14', icon: 'clock-circle' },
              SUCCESS: { text: '通过', color: '#52c41a', icon: 'check-circle' },
              FAILED: { text: '未通过', color: '#f5222d', icon: 'close-circle' },
              SKIPPED: { text: '已跳过', color: '#d9d9d9', icon: 'stop' },
              TERMINATING: { text: '终止中', color: '#ff7a45', icon: 'stop', spin: true },
              MIXED: { text: '部分通过', color: '#faad14', icon: 'exclamation-circle' }
            };

            // 使用主机的状态
            const hostStatus = row.statusStr || row.status || '';

            // 如果主机有状态，直接显示
            if (hostStatus && statusMap[hostStatus]) {
              const status = statusMap[hostStatus];
              return h('span', {
                class: 'flex-container',
                style: {
                  display: 'flex',
                  alignItems: 'center',
                  color: status.color
                }
              }, [
                h('a-icon', {
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [status.text])
              ]);
            }

            return h('span', {}, ['-']);
          },
        },
        {
          title: "检查项",
          key: "checkItem",
          width: "18%",  // 增加检查项列宽度
          customRender: (text, row) => {
            const h = this.$createElement;

            // 状态映射
            const statusMap = {
              CHECKING: { text: '检查中', color: '#1890ff', icon: 'loading' },
              WAITING: { text: '等待检查', color: '#faad14', icon: 'clock-circle' },
              SUCCESS: { text: '通过', color: '#52c41a', icon: 'check-circle' },
              FAILED: { text: '未通过', color: '#f5222d', icon: 'close-circle' },
              SKIPPED: { text: '已跳过', color: '#d9d9d9', icon: 'stop' },
              TERMINATING: { text: '终止中', color: '#ff7a45', icon: 'stop', spin: true },
              MIXED: { text: '部分通过', color: '#faad14', icon: 'exclamation-circle' }
            };

            // 检查主机是否有检查项
            const checkItems = row.checkItems || [];

            // 优先级处理：检查中 > 待检查 > 失败 > 跳过 > 成功

            // 1. 先检查是否有正在检查中的项目
            const currentItem = checkItems.find(item => item.status === 'CHECKING');
            if (currentItem) {
              const status = statusMap[currentItem.status];
              return h('span', {
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', {
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [currentItem.itemName])
              ]);
            }

            // 2. 其次检查是否有待检查的项目
            const waitingItem = checkItems.find(item => item.status === 'WAITING');
            if (waitingItem) {
              const status = statusMap[waitingItem.status];
              return h('span', {
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', {
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [waitingItem.itemName])
              ]);
            }

            // 3. 查找失败的项目，并显示最后一个失败项
            const failedItems = checkItems.filter(item => item.status === 'FAILED');
            if (failedItems.length > 0) {
              const lastFailedItem = failedItems[failedItems.length - 1];
              const status = statusMap[lastFailedItem.status];
              return h('span', {
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', {
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [lastFailedItem.itemName])
              ]);
            }

            // 4. 查找跳过的项目，显示最后一个跳过项
            const skippedItems = checkItems.filter(item => item.status === 'SKIPPED');
            if (skippedItems.length > 0) {
              const lastSkippedItem = skippedItems[skippedItems.length - 1];
              const status = statusMap[lastSkippedItem.status];
              return h('span', {
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', {
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [lastSkippedItem.itemName])
              ]);
            }

            // 5. 最后查找成功的项目，显示最后一个成功项
            const successItems = checkItems.filter(item => item.status === 'SUCCESS');
            if (successItems.length > 0) {
              const lastSuccessItem = successItems[successItems.length - 1];
              const status = statusMap[lastSuccessItem.status];
              return h('span', {
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', {
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [lastSuccessItem.itemName])
              ]);
            }

            // 如果没有任何检查项，则显示占位符
            return h('span', {}, ['-']);
          },
        },
        {
          title: "操作",
          key: "action",
          width: "10%",
          customRender: (text, row) => {
            const h = this.$createElement;
            const isChecking = row.status === 'CHECKING' || row.statusStr === 'CHECKING';
            const isWaiting = row.status === 'WAITING' || row.statusStr === 'WAITING';

            return h('div', { class: 'action-buttons apple-actions' }, [
              // 终止按钮 - 检查中时显示
              isChecking ? h('a-button', {
                attrs: {
                  type: 'danger',
                  size: 'small'
                },
                class: 'apple-button danger',
                on: {
                  click: () => this.stopCheck(row)
                }
              }, ["终止"]) : null,

              // 重试按钮 - 非检查中且非等待检查时显示
              !isChecking ? h('a-button', {
                attrs: {
                  type: 'link',
                  size: 'small',
                  disabled: isWaiting // 等待检查时禁用
                },
                class: 'apple-button primary',
                on: {
                  click: () => this.retryEnvironment(row)
                }
              }, ["重试"]) : null
            ].filter(Boolean));
          },
        }
      ],
      selectedCheckItems: {}, // 存储每个主机选中的检查项 { hostname: [itemName1, itemName2] }
      logVisible: false,
      logModalTitle: '',
      currentLogIp: null,
      currentLogItemId: null,
      currentLogItemName: null,
      checkItem: null,
      forceUseTypedApi: false,
      hostnameEditVisible: false,
      currentEditHost: null,
      newHostname: '',
      editLoading: false,
    };
  },
  computed: {
    // 计算是否有任何主机的检查项正在检查中
    hasAnyCheckingItems() {
      if (!this.dataSource || this.dataSource.length === 0) {
        return false;
      }

      return this.dataSource.some(host => {
        const checkItems = this.checkItemsMap[host.ip] || [];
        return checkItems.some(item => item.status === 'CHECKING');
      });
    }
  },
  methods: {
    /**
     * 检查状态是否匹配目标状态
     * 支持status为collecting或loading时与loading目标状态匹配
     * @param {string} status 当前状态
     * @param {string} targetStatus 目标状态
     * @returns {boolean} 是否匹配
     */
    checkStatus(status, targetStatus) {
      if (!status) return false

      // 处理大小写兼容
      const statusLower = status.toLowerCase()
      const targetLower = targetStatus.toLowerCase()

      // 特殊处理loading状态，collecting也视为loading
      if (targetLower === 'loading') {
        return statusLower === 'loading' || statusLower === 'collecting'
      }

      return statusLower === targetLower
    },

    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.pollingSearch();
    },
    getEnvironmentList(flag) {
      // 只在请求进行中时跳过新的请求
      if (this.isRequesting) {
        console.log('上一次请求还在进行中，跳过本次请求');
        return;
      }

      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId,
        ...this.steps1Data,
        // 将hosts参数的值作为ips参数的值
        ips: this.steps1Data.hosts
      };


      this.isRequesting = true;

      this.$axiosPost(global.API.analysisHostList, params)
          .then((res) => {
            if (res.code === 200) {
              // 处理主机状态
              if (res.data && res.data.length > 0) {
                res.data.forEach(host => {
                  // 计算主机状态
                  host.status = this.calculateHostStatus(host);

                  // 将返回的checkItems数据保存到checkItemsMap中
                  if (host.checkItems) {
                    this.$set(this.checkItemsMap, host.ip, host.checkItems);
                  }

                  // 检查SSH连接状态
                  if (host.checkItems && host.checkItems.length > 0) {
                    // 查找SSH相关的检查项，如果有且状态为FAILED，则标记SSH连接失败
                    const sshCheckItems = host.checkItems.filter(item =>
                        item.itemCode === 'SSH_CONNECTION' ||
                        item.itemName.includes('SSH') ||
                        item.itemCode === 'PASSWORD_FREE');

                    if (sshCheckItems.length > 0 && sshCheckItems.some(item => item.status === 'FAILED')) {
                      host.hasSSHError = true;
                      host.sshConnectStatus = 'error';
                    }
                  }

                  // 如果osInfoStatus为error，也标记SSH可能有问题
                  if (host.osInfoStatus === 'error') {
                    host.hasSSHError = true;
                    host.sshConnectStatus = 'error';
                  }

                  // 如果sshStatus字段存在并且状态为error，也标记SSH连接失败
                  if (host.sshStatus === 'error' || host.sshStatus === 'ERROR' ||
                      host.sshConnectStatus === 'error' || host.sshConnectStatus === 'ERROR') {
                    host.hasSSHError = true;
                    host.sshConnectStatus = 'error';
                    // 如果正在获取OS信息，则将状态改为error而非loading
                    if (host.osInfoStatus === 'loading') {
                      host.osInfoStatus = 'error';
                    }
                  }
                });

                // 检查是否有主机正在进行检查
                this.updateCheckingStatus(res.data);

                // 检查是否有主机已完成检查，用于判断是否已开始过检查
                if (res.data && res.data.length > 0) {
                  const hasCompletedChecks = res.data.some(host => {
                    // 只要有一个主机的检查项不是WAITING状态，说明已经开始过检查
                    const checkItems = host.checkItems || [];
                    return checkItems.length > 0 &&
                        checkItems.some(item => item.status !== 'WAITING');
                  });

                  if (hasCompletedChecks) {
                    this.hasStartedCheck = true;
                  }
                }
              }

              this.dataSource = res.data;
              this.pagination.total = res.total;

              // 保存队列状态信息
              if (res.queueStatus) {
                this.queueStatus = res.queueStatus;
              }

              if (this.depType=='K8S'){
                let data = JSON.parse(JSON.stringify(res.data))
                data && data.forEach(e => {
                  if (e.checkResult.code=='10001'){
                    e['CheckResult'] = e.checkResult
                    delete e.checkResult
                    let arr=[]
                    arr[0] = e
                    this.saveK8sHostApi(arr)
                  }
                })
              }
            }
          })
          .catch((error) => {
            console.error('请求失败:', error);
          })
          .finally(() => {
            // 无论成功还是失败，都要重置状态
            this.loading = false;
            this.isRequesting = false;
          });
    },

    /**
     * 启动批量检查主机
     * 提取主机名列表，调用后端接口开始检查
     */
    async startHostCheck() {
      // 调用开始检查API，只传递clusterId参数
      try {
        const params = {
          clusterId: this.clusterId
        };

        const res = await this.$axiosPost(global.API.startHostCheck, params);
        if (res.code === 200) {
          this.$message.success('已开始主机检查');
          this.isCheckingActive = true;

          // 立即刷新一次，不等待自动刷新
          this.getEnvironmentList(false);
        } else {
          this.$message.error('开始主机检查失败: ' + res.msg);
        }
      } catch (err) {
        console.error('调用开始检查API失败:', err);
        this.$message.error('开始主机检查出错');
      }
    },

    /**
     * 终止批量检查主机
     */
    async stopHostCheck() {
      // 调用终止检查API
      try {
        const params = {
          clusterId: this.clusterId
        };

        const res = await this.$axiosPost(global.API.stopHostCheck, params);
        if (res.code === 200) {
          this.$message.success(res.msg || '已终止主机检查');

          // 立即刷新一次，不等待自动刷新
          this.getEnvironmentList(false);
        } else {
          this.$message.error('终止主机检查失败: ' + res.msg);
        }
      } catch (err) {
        console.error('调用终止检查API失败:', err);
        this.$message.error('终止主机检查出错');
      }
    },

    /**
     * 提取主机名列表，调用后端接口开始检查
     */
    async startBatchCheckHosts(hosts) {
      if (!hosts || hosts.length === 0) {
        return;
      }

      // 提取需要检查的主机IP列表（而不是主机名）
      // 只对未受管或状态为WAITING的主机进行检查
      const ipsToCheck = hosts
          .filter(host => !host.managed || host.status === 'WAITING')
          .map(host => host.ip);

      if (ipsToCheck.length === 0) {
        console.log('没有需要检查的主机');
        return;
      }

      // 调用批量检查API
      try {
        const res = await this.$axiosJsonPost(global.API.batchCheckHosts + '?clusterId=' + this.clusterId, ipsToCheck);
        if (res.code === 200) {
          console.log('成功启动主机检查:', res.msg);

          // 立即刷新一次，不等待5秒后的自动刷新
          this.getEnvironmentList(false);
        } else {
          console.warn('启动主机检查失败:', res.msg);
        }
      } catch (err) {
        console.error('调用批量检查API失败:', err);
      }
    },

    // 计算主机的整体状态
    calculateHostStatus(host) {
      // 如果主机已经有状态则返回
      if (host.statusStr || host.status) return host.statusStr || host.status;

      // 没有检查项则返回空状态
      const checkItems = host.checkItems || [];
      if (checkItems.length === 0) return null;

      // 如果有检查中的项，则状态为"检查中"
      if (checkItems.some(item => item.status === 'CHECKING')) {
        return 'CHECKING';
      }

      // 如果有等待检查的项，则状态为"等待检查"
      if (checkItems.some(item => item.status === 'WAITING')) {
        return 'WAITING';
      }

      // 如果有失败的项，则状态为"未通过"
      if (checkItems.some(item => item.status === 'FAILED')) {
        return 'FAILED';
      }

      // 如果所有项都是"跳过"，则状态为"已跳过"
      if (checkItems.every(item => item.status === 'SKIPPED')) {
        return 'SKIPPED';
      }

      // 如果有的是跳过有的是成功，则状态为"部分通过"
      if (checkItems.some(item => item.status === 'SKIPPED') &&
          checkItems.some(item => item.status === 'SUCCESS')) {
        return 'MIXED';
      }

      // 默认情况：所有项都通过
      return 'SUCCESS';
    },
    saveK8sHostApi (params){
      this.$axiosJsonPost(global.API.saveK8sHost + '?clusterId=' + this.clusterId, params).then((res) => {
      });
    },
    //表格选择
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
    retryEnvironment(row) {
      let ips = "";
      if (row === "all") {
        // 不再需要检查选择的行
        ips = this.selectedRowKeys.join(",");
      } else {
        ips = row.ip;
      }
      const params = {
        ips,
        clusterId: this.clusterId,
        sshUser: this.steps1Data.sshUser,
        sshPort: this.steps1Data.sshPort,
      };
      this.$axiosPost(global.API.rehostCheck, params).then((res) => {
        this.selectedRowKeys = [];
        this.$message.success(`操作成功`);
        this.pollingSearch();
      });
    },
    // 主机环境校验是否完成 是否可以进入下一步
    async hostCheckCompleted(callback) {
      const params = {
        clusterId: this.clusterId,
      };
      // 等待网络请求结束
      let flag = await this.$axiosPost(global.API.hostCheckCompleted, params);
      // 网络请求结束后才执行下边的语句  如果传入的callback方法为空或者没传内容也不会去执行，这样也不会影响此方法在别处的调用
      if (callback) {
        callback(flag);
      }
    },
    // 展开行渲染函数
    expandedRowRender(record) {
      const h = this.$createElement;
      const checkItems = this.checkItemsMap[record.ip] || [];

      // 判断主机是否处于检查中状态
      const isHostChecking = record.status === 'CHECKING' || record.statusStr === 'CHECKING';

      const columns = [
        {
          title: '检查项',
          dataIndex: 'itemName',
          key: 'itemName',
          width: '25%',
          customRender: (text) => {
            const h = this.$createElement;
            return h('div', {
              style: {
                fontSize: '14px',
                fontWeight: '500',
                color: '#1d1d1f'
              }
            }, [text]);
          }
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '15%',
          customRender: (text) => {
            const h = this.$createElement;
            // 使用字符串类型的状态映射
            const statusMap = {
              'WAITING': { text: '待检查', color: '#FF9500', bgColor: 'rgba(255, 149, 0, 0.1)', icon: 'clock-circle' },
              'SUCCESS': { text: '通过', color: '#34C759', bgColor: 'rgba(52, 199, 89, 0.1)', icon: 'check-circle' },
              'FAILED': { text: '未通过', color: '#FF3B30', bgColor: 'rgba(255, 59, 48, 0.1)', icon: 'close-circle' },
              'CHECKING': { text: '检查中', color: '#007AFF', bgColor: 'rgba(0, 122, 255, 0.1)', icon: 'loading' },
              'SKIPPED': { text: '已跳过', color: '#8E8E93', bgColor: 'rgba(142, 142, 147, 0.1)', icon: 'warning' },
              'TERMINATING': { text: '终止中', color: '#FF9500', bgColor: 'rgba(255, 149, 0, 0.1)', icon: 'stop', spin: true },
              'FIXING': { text: '修复中', color: '#5856D6', bgColor: 'rgba(88, 86, 214, 0.1)', icon: 'tool', spin: true }
            };

            const status = statusMap[text] || { text: '未知', color: '#8E8E93', bgColor: 'rgba(142, 142, 147, 0.1)', icon: 'question-circle' };

            return h('div', {
              style: {
                display: 'inline-flex',
                alignItems: 'center',
                padding: '4px 12px',
                borderRadius: '12px',
                backgroundColor: status.bgColor,
                fontSize: '13px',
                fontWeight: '500',
                color: status.color,
                transition: 'all 0.2s ease'
              }
            }, [
              h('a-icon', {
                props: {
                  type: status.icon,
                  spin: status.spin || status.icon === 'loading'
                },
                style: {
                  marginRight: '6px',
                  fontSize: '14px'
                }
              }),
              status.text
            ]);
          }
        },
        {
          title: '检查结果',
          dataIndex: 'message',
          key: 'message',
          width: '20%',
          customRender: (text, row) => {
            const h = this.$createElement;

            // 检查文本是否存在
            if (!text) return h('span', {}, ['-']);

            // 不再限制文本长度，所有检查项都显示悬浮提示
            const displayText = text;

            // 根据状态设置颜色
            const statusColor = row.status === 'SUCCESS' ? '#34C759' :
                row.status === 'FAILED' ? '#FF3B30' :
                    row.status === 'SKIPPED' ? '#8E8E93' :
                        row.status === 'CHECKING' ? '#007AFF' :
                            row.status === 'FIXING' ? '#5856D6' : '#1d1d1f';

            const statusIcon = row.status === 'SUCCESS' ? 'check-circle' :
                row.status === 'FAILED' ? 'close-circle' :
                    row.status === 'SKIPPED' ? 'warning' :
                        row.status === 'CHECKING' ? 'loading' :
                            row.status === 'FIXING' ? 'tool' : 'info-circle';

            const statusSpin = row.status === 'CHECKING' || row.status === 'FIXING';
            const statusTheme = row.status !== 'CHECKING' && row.status !== 'FIXING' ? 'filled' : undefined;

            // 创建一个更符合苹果设计风格的tooltip内容
            const tooltipContent = h('div', {
              class: 'check-result-tooltip',
              style: {
                maxWidth: '1200px',
                padding: '0',
                borderRadius: '16px',
                background: '#ffffff',
                color: '#1d1d1f',
                fontSize: '14px',
                lineHeight: '1.6',
                wordBreak: 'break-word',
                whiteSpace: 'pre-wrap',
                fontFamily: '"SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif',
                overflow: 'hidden',
                border: 'none',
                boxShadow: '0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05)'
              }
            }, [
              // 添加标题行
              h('div', {
                style: {
                  fontWeight: '500',
                  padding: '12px 16px',
                  borderBottom: '1px solid rgba(0, 0, 0, 0.05)',
                  backgroundColor: statusColor,
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }
              }, [
                h('div', {
                  style: {
                    display: 'flex',
                    alignItems: 'center'
                  }
                }, [
                  h('a-icon', {
                    props: {
                      type: statusIcon,
                      theme: statusTheme,
                      spin: statusSpin
                    },
                    style: {
                      marginRight: '8px',
                      fontSize: '16px'
                    }
                  }),
                  '检查结果详情'
                ])
              ]),
              // 添加检查结果内容
              h('div', {
                domProps: {
                  innerHTML: text
                },
                style: {
                  fontSize: '14px',
                  lineHeight: '1.6',
                  padding: '16px',
                  maxHeight: '70vh',
                  overflowY: 'auto'
                }
              })
            ]);

            // 使用tooltip组件但覆盖默认样式
            return h('a-tooltip', {
              props: {
                title: tooltipContent,
                placement: 'top',
                mouseEnterDelay: 0.3,
                overlayClassName: 'apple-style-tooltip',
                autoAdjustOverflow: true,
                arrowPointAtCenter: true,
                color: '#ffffff',
                getPopupContainer: () => document.body
              },
              class: 'custom-tooltip'
            }, [
              h('span', {
                style: {
                  cursor: 'pointer',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  display: 'inline-block',
                  maxWidth: '100%',
                  color: statusColor,
                  transition: 'all 0.3s',
                  fontSize: '14px',
                  padding: '4px 10px',
                  borderRadius: '20px',
                  backgroundColor: `${statusColor}15`, // 15是透明度，相当于rgba的0.1
                  border: `1px solid ${statusColor}30` // 30是透明度，相当于rgba的0.2
                },
                class: 'check-result-text'
              }, [
                // 状态图标
                h('a-icon', {
                  props: {
                    type: statusIcon,
                    theme: statusTheme,
                    spin: statusSpin
                  },
                  style: {
                    marginRight: '4px',
                    fontSize: '12px'
                  }
                }),
                // 在这里使用一个辅助函数去除HTML标签，只显示纯文本
                h('span', {}, [
                  displayText.length > 20 ?
                      this.stripHtml(displayText).substr(0, 20) + '...' :
                      this.stripHtml(displayText)
                ])
              ])
            ]);
          }
        },
        {
          title: '操作',
          key: 'action',
          width: '25%',
          customRender: (text, row) => {
            const h = this.$createElement;
            const isChecking = row.status === 'CHECKING';
            const isFailed = row.status === 'FAILED';

            return h('div', {
              class: 'action-buttons',
              style: {
                display: 'flex',
                gap: '8px'
              }
            }, [
              // 终止按钮 - 检查中时显示
              isChecking ? h('button', {
                style: {
                  border: 'none',
                  backgroundColor: 'rgba(255, 59, 48, 0.1)',
                  color: '#FF3B30',
                  padding: '6px 12px',
                  borderRadius: '12px',
                  fontSize: '13px',
                  fontWeight: '500',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                },
                on: {
                  click: () => this.stopCheckItem(record.ip, row.id)
                }
              }, [
                h('a-icon', {
                  props: { type: 'close' },
                  style: { marginRight: '4px', fontSize: '12px' }
                }),
                "终止"
              ]) : null,

              // 重试按钮 - 非检查中时显示，检查中则禁用
              !isChecking ? h('button', {
                style: {
                  border: 'none',
                  backgroundColor: 'rgba(0, 122, 255, 0.1)',
                  color: '#007AFF',
                  padding: '6px 12px',
                  borderRadius: '12px',
                  fontSize: '13px',
                  fontWeight: '500',
                  cursor: row.status === 'CHECKING' || row.status === 'FIXING' ||
                  !((row.status === 'FAILED' || row.status === 'SUCCESS' || row.status === 'SKIPPED')) ? 'not-allowed' : 'pointer',
                  transition: 'all 0.2s ease',
                  opacity: row.status === 'CHECKING' || row.status === 'FIXING' ||
                  !((row.status === 'FAILED' || row.status === 'SUCCESS' || row.status === 'SKIPPED')) ? '0.5' : '1',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                },
                attrs: {
                  disabled: row.status === 'CHECKING' || row.status === 'FIXING' ||
                      !((row.status === 'FAILED' || row.status === 'SUCCESS' || row.status === 'SKIPPED'))
                },
                on: {
                  click: () => this.retryCheckItem(record.ip, row.id)
                }
              }, [
                h('a-icon', {
                  props: { type: 'redo' },
                  style: { marginRight: '4px', fontSize: '12px' }
                }),
                "重试"
              ]) : null,

              // 修复按钮 - 失败时可用，主机整体检查中时禁用
              isFailed ? h('button', {
                style: {
                  border: 'none',
                  backgroundColor: 'rgba(88, 86, 214, 0.1)',
                  color: '#5856D6',
                  padding: '6px 12px',
                  borderRadius: '12px',
                  fontSize: '13px',
                  fontWeight: '500',
                  cursor: isHostChecking || row.status === 'FIXING' ? 'not-allowed' : 'pointer',
                  transition: 'all 0.2s ease',
                  opacity: isHostChecking || row.status === 'FIXING' ? '0.5' : '1',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                },
                attrs: {
                  disabled: isHostChecking || row.status === 'FIXING'
                },
                on: {
                  click: () => this.fixCheckItem(record.ip, row)
                }
              }, [
                h('a-icon', {
                  props: { type: 'tool' },
                  style: { marginRight: '4px', fontSize: '12px' }
                }),
                "修复"
              ]) : null,

              // 跳过按钮 - 失败时可用，主机整体检查中时禁用
              isFailed ? h('button', {
                style: {
                  border: 'none',
                  backgroundColor: 'rgba(142, 142, 147, 0.1)',
                  color: '#8E8E93',
                  padding: '6px 12px',
                  borderRadius: '12px',
                  fontSize: '13px',
                  fontWeight: '500',
                  cursor: isHostChecking ? 'not-allowed' : 'pointer',
                  transition: 'all 0.2s ease',
                  opacity: isHostChecking ? '0.5' : '1',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                },
                attrs: {
                  disabled: isHostChecking
                },
                on: {
                  click: () => this.skipCheckItem(record.ip, row.id)
                }
              }, [
                h('a-icon', {
                  props: { type: 'forward' },
                  style: { marginRight: '4px', fontSize: '12px' }
                }),
                "跳过"
              ]) : null
            ].filter(Boolean));
          }
        },
        {
          title: '日志',
          key: 'log',
          width: '15%',
          customRender: (text, row) => {
            const h = this.$createElement;
            return h('button', {
              style: {
                border: 'none',
                backgroundColor: 'rgba(0, 122, 255, 0.1)',
                color: '#007AFF',
                padding: '6px 12px',
                borderRadius: '12px',
                fontSize: '13px',
                fontWeight: '500',
                cursor: row.status === 'WAITING' ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s ease',
                opacity: row.status === 'WAITING' ? '0.5' : '1',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              },
              attrs: {
                disabled: row.status === 'WAITING'
              },
              on: {
                click: () => this.viewItemLog(record.ip, row.id, row.itemName)
              }
            }, [
              h('a-icon', {
                props: { type: 'file-text' },
                style: { marginRight: '4px', fontSize: '12px' }
              }),
              "查看日志"
            ]);
          }
        }
      ];

      // 创建header-summary部分
      const headerSummary = h('div', {
        class: 'header-summary',
        style: {
          fontFamily: '"SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif',
          fontSize: '16px',
          display: 'flex',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px'
        }
      }, [
        h('span', {
          style: {
            fontWeight: '600',
            color: '#1d1d1f'
          }
        }, [`共 ${checkItems.length} 项检查`]),

        h('div', {
          style: {
            display: 'flex',
            alignItems: 'center',
            padding: '4px 12px',
            borderRadius: '12px',
            backgroundColor: 'rgba(52, 199, 89, 0.1)',
            color: '#34C759',
            fontSize: '14px',
            fontWeight: '500'
          }
        }, [
          h('a-icon', {
            props: { type: 'check-circle' },
            style: { marginRight: '6px' }
          }),
          `${checkItems.filter(item => item.status === 'SUCCESS').length} 项通过`
        ]),

        h('div', {
          style: {
            display: 'flex',
            alignItems: 'center',
            padding: '4px 12px',
            borderRadius: '12px',
            backgroundColor: 'rgba(255, 59, 48, 0.1)',
            color: '#FF3B30',
            fontSize: '14px',
            fontWeight: '500'
          }
        }, [
          h('a-icon', {
            props: { type: 'close-circle' },
            style: { marginRight: '6px' }
          }),
          `${checkItems.filter(item => item.status === 'FAILED').length} 项失败`
        ]),

        h('div', {
          style: {
            display: 'flex',
            alignItems: 'center',
            padding: '4px 12px',
            borderRadius: '12px',
            backgroundColor: 'rgba(255, 149, 0, 0.1)',
            color: '#FF9500',
            fontSize: '14px',
            fontWeight: '500'
          }
        }, [
          h('a-icon', {
            props: { type: 'clock-circle' },
            style: { marginRight: '6px' }
          }),
          `${checkItems.filter(item => item.status === 'WAITING').length} 项待检查`
        ]),

        h('div', {
          style: {
            display: 'flex',
            alignItems: 'center',
            padding: '4px 12px',
            borderRadius: '12px',
            backgroundColor: 'rgba(0, 122, 255, 0.1)',
            color: '#007AFF',
            fontSize: '14px',
            fontWeight: '500'
          }
        }, [
          h('a-icon', {
            props: {
              type: 'loading',
              spin: true
            },
            style: { marginRight: '6px' }
          }),
          `${checkItems.filter(item => item.status === 'CHECKING').length} 项检查中`
        ]),

        h('div', {
          style: {
            display: 'flex',
            alignItems: 'center',
            padding: '4px 12px',
            borderRadius: '12px',
            backgroundColor: 'rgba(142, 142, 147, 0.1)',
            color: '#8E8E93',
            fontSize: '14px',
            fontWeight: '500'
          }
        }, [
          h('a-icon', {
            props: { type: 'warning' },
            style: { marginRight: '6px' }
          }),
          `${checkItems.filter(item => item.status === 'SKIPPED').length} 项已跳过`
        ])
      ]);

      // 创建header-actions部分
      const headerActions = h('div', {
        class: 'header-actions',
        style: {
          display: 'flex',
          gap: '12px'
        }
      }, [
        h('button', {
          style: {
            backgroundColor: '#007AFF',
            color: 'white',
            border: 'none',
            borderRadius: '12px',
            padding: '8px 16px',
            fontSize: '14px',
            fontWeight: '500',
            cursor: !this.hasRetryableSelectedItems(record.ip) ? 'not-allowed' : 'pointer',
            opacity: !this.hasRetryableSelectedItems(record.ip) ? '0.6' : '1',
            transition: 'all 0.2s ease',
            display: 'flex',
            alignItems: 'center'
          },
          attrs: {
            disabled: !this.hasRetryableSelectedItems(record.ip)
          },
          on: {
            click: () => this.retrySelectedItems(record.ip)
          }
        }, [
          h('a-icon', {
            props: { type: 'redo' },
            style: { marginRight: '6px' }
          }),
          '重试选中项'
        ]),

        h('button', {
          style: {
            backgroundColor: '#5856D6',
            color: 'white',
            border: 'none',
            borderRadius: '12px',
            padding: '8px 16px',
            fontSize: '14px',
            fontWeight: '500',
            cursor: !this.hasFixableSelectedItems(record.ip) ? 'not-allowed' : 'pointer',
            opacity: !this.hasFixableSelectedItems(record.ip) ? '0.6' : '1',
            transition: 'all 0.2s ease',
            display: 'flex',
            alignItems: 'center'
          },
          attrs: {
            disabled: !this.hasFixableSelectedItems(record.ip)
          },
          on: {
            click: () => this.fixSelectedItems(record.ip)
          }
        }, [
          h('a-icon', {
            props: { type: 'tool' },
            style: { marginRight: '6px' }
          }),
          '修复选中项'
        ])
      ]);

      // 创建表格容器，添加苹果风格的样式
      return h('div', {
        class: 'check-items-container',
        style: {
          padding: '24px',
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          boxShadow: '0 2px 10px rgba(0, 0, 0, 0.05)',
          margin: '0px',
          fontFamily: '"SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif'
        }
      }, [
        h('div', {
          class: 'check-items-header',
          style: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '20px',
            flexWrap: 'wrap',
            gap: '16px'
          }
        }, [
          headerSummary,
          headerActions
        ]),

        // 添加分隔线
        h('div', {
          style: {
            height: '1px',
            backgroundColor: 'rgba(0, 0, 0, 0.05)',
            margin: '0 0 20px 0'
          }
        }),

        h('a-table', {
          props: {
            columns: columns,
            dataSource: checkItems,
            pagination: false,
            size: 'middle',
            rowKey: 'id',
            rowSelection: {
              selectedRowKeys: this.selectedCheckItems[record.ip] || [],
              onChange: (selectedRowKeys) => this.onCheckItemSelect(record.ip, selectedRowKeys)
            }
          },
          class: 'apple-style-table',
          style: {
            borderRadius: '12px',
            overflow: 'hidden'
          }
        })
      ]);
    },

    // 选择检查项
    onCheckItemSelect(ip, selectedRowKeys) {
      this.$set(this.selectedCheckItems, ip, selectedRowKeys);
    },

    // 重试选中的检查项
    async retrySelectedItems(ip) {
      const selectedItems = this.selectedCheckItems[ip] || [];
      if (selectedItems.length === 0) {
        this.$message.warning('请选择要重试的检查项');
        return;
      }

      try {
        const res = await this.$axiosPost(global.API.retryCheckItems, {
          clusterId: this.clusterId,
          ip,
          itemNames: selectedItems
        });

        if (res.code === 200) {
          this.$message.success('重试指令已发送');
          // 立即刷新一次，不等待5秒后的自动刷新
          this.getEnvironmentList(false);
        } else {
          this.$message.error(res.msg || '重试检查项失败');
        }
      } catch (error) {
        console.error('重试检查项失败:', error);
        this.$message.error('重试检查项失败');
      }
    },

    // 跳过检查项
    async skipCheckItem(ip, itemId) {
      // 查找对应的主机信息
      const host = this.dataSource.find(h => h.ip === ip);

      // 检查主机是否处于检查中状态
      if (host && (host.status === 'CHECKING' || host.statusStr === 'CHECKING')) {
        this.$message.warning('主机当前正在检查中，请稍后再尝试跳过');
        return;
      }

      try {
        const res = await this.$axiosPost(global.API.skipCheckItem, {
          clusterId: this.clusterId,
          ip: ip,
          itemId: itemId
        });
        if (res.code === 200) {
          this.$message.success('已跳过该检查项');

          // 更新检查项状态
          const items = this.checkItemsMap[ip];
          if (items) {
            const targetItem = items.find(item => item.id === itemId);
            if (targetItem) {
              targetItem.status = 'SKIPPED';
              targetItem.message = '已手动跳过此检查项';

              // 使用...items创建新数组，确保Vue能检测到变化
              this.$set(this.checkItemsMap, ip, [...items]);

              // 延迟1秒后刷新列表获取服务器最新状态
              setTimeout(() => {
                this.refreshHostList();
              }, 1000);
            }
          }
        }
      } catch (error) {
        console.error('跳过检查项失败:', error);
        this.$message.error('跳过检查项失败');
      }
    },

    // 获取主机校验项
    async getHostCheckItems(ip, isFirstHost = false) {
      try {
        const res = await this.$axiosGet(global.API.getHostCheckItems + '?ip=' + ip + '&clusterId=' + this.clusterId);
        if (res.code === 200) {
          // 设置检查项数据
          this.$set(this.checkItemsMap, ip, res.data);
        }
      } catch (error) {
        console.error('获取主机校验项失败:', error);
        this.$message.error('获取主机校验项失败');
      }
    },

    // 使用自定义a-modal组件实现确认弹窗
    async fixCheckItem(ip, item) {
      if (item.status !== 'FAILED') return;

      // 查找对应的主机信息
      const host = this.dataSource.find(h => h.ip === ip);

      // 删除以下检查，允许在主机检查过程中也能点击修复按钮
      /*
      if (host && (host.status === 'CHECKING' || host.statusStr === 'CHECKING')) {
        this.$message.warning('主机当前正在检查中，请稍后再尝试修复');
        return;
      }
      */

      try {
        // 获取确认信息
        const res = await this.$axiosGet(global.API.getCheckItemConfirmInfo, {
          clusterId: this.clusterId,
          ip: ip,
          itemId: item.id
        });

        console.log('获取确认信息结果:', res); // 添加调试日志

        if (res.code === 200) {
          const needConfirm = res.needConfirm;
          const confirmMessage = res.confirmMessage;
          const itemName = res.itemName || item.itemName;

          if (needConfirm) {
            // 设置弹窗数据
            this.fixConfirmTitle = '确认修复 - ' + itemName;
            this.fixConfirmContent = confirmMessage;
            this.fixConfirmIp = ip;
            this.fixConfirmItem = item;

            // 显示弹窗
            this.fixConfirmVisible = true;
          } else {
            // 无需确认，直接修复
            this.doFixCheckItem(ip, item, false);
          }
        } else {
          // 处理API错误
          this.$message.error(res.msg || '获取确认信息失败');
        }
      } catch (err) {
        console.error('获取确认信息失败:', err);
        this.$message.error('获取确认信息失败');
      }
    },

    // 处理确认修复对话框的确认按钮
    handleFixConfirm() {
      this.fixConfirmLoading = true;

      // 执行修复
      this.doFixCheckItem(this.fixConfirmIp, this.fixConfirmItem, true).then(() => {
        this.fixConfirmVisible = false;
        this.fixConfirmLoading = false;
      }).catch(() => {
        this.fixConfirmLoading = false;
      });
    },

    // 处理取消对话框
    handleFixCancel() {
      this.fixConfirmVisible = false;
    },

    // 执行实际的修复操作
    async doFixCheckItem(ip, item, skipConfirm) {
      console.log('执行修复操作:', ip, item.id, skipConfirm); // 添加调试日志

      // Set loading state
      this.$set(item, 'fixing', true);

      try {
        // Call fix API
        const res = await this.$axiosPost(global.API.fixCheckItem, {
          clusterId: this.clusterId,
          ip: ip,
          itemId: item.id,
          skipConfirm: skipConfirm
        });

        if (res.code === 200) {
          this.$message.success('修复操作已提交');
          // Refresh check item status after a delay
          setTimeout(() => {
            this.getHostCheckItems(ip);
          }, 1000);
        }

        // 清理状态
        this.$set(item, 'fixing', false);
        return res;
      } catch (err) {
        console.error('修复失败:', err);
        // 只在网络错误等前端异常情况下显示通用错误消息
        this.$message.error('请求失败，请检查网络连接');
        this.$set(item, 'fixing', false);
        throw err;
      }
    },

    // 修复所有检查项
    async fixAllCheckItems(ip) {
      try {
        const res = await this.$axiosPost(global.API.fixAllCheckItems, {
          clusterId: this.clusterId,
          ip
        });
        if (res.code === 200) {
          this.$message.success('修复指令已发送');
          // 通过轮询获取最新状态
          this.pollingSearch();
        }
      } catch (error) {
        console.error('修复所有检查项失败:', error);
        this.$message.error('修复所有检查项失败');
      }
    },

    // 添加重试单个检查项的方法
    async retryCheckItem(ip, itemId) {
      // 查找对应的主机信息
      const host = this.dataSource.find(h => h.ip === ip);

      // 删除以下检查，允许在主机检查过程中也能点击重试按钮
      /*
      if (host && (host.status === 'CHECKING' || host.statusStr === 'CHECKING')) {
        this.$message.warning('主机当前正在检查中，请稍后再尝试重试');
        return;
      }
      */

      try {
        const res = await this.$axiosPost(global.API.retryCheckItems, {
          clusterId: this.clusterId,
          ip: ip,
          itemNames: [itemId]  // 只重试选中的单个检查项
        });

        if (res.code === 200) {
          this.$message.success('重试指令已发送');

          // 立即刷新一次，不等待5秒后的自动刷新
          this.getEnvironmentList(false);
        } else {
          this.$message.error(res.msg || '重试检查项失败');
        }
      } catch (error) {
        console.error('重试检查项失败:', error);
        this.$message.error('重试检查项失败');
      }
    },

    // 自定义展开图标
    customExpandIcon({ expanded, onExpand, record }) {
      const h = this.$createElement;
      return h('div', {
        class: ['apple-expand-icon', expanded ? 'expanded' : ''],
        on: {
          click: (e) => {
            onExpand(record, e);
          }
        }
      }, [
        h('div', { class: 'expand-icon-inner' }, [
          h('a-icon', {
            props: {
              type: 'right'
            },
            style: {
              fontSize: '12px',
              transition: 'transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)'
            }
          })
        ])
      ]);
    },

    // 检查是否有可修复的选中项
    hasFixableSelectedItems(ip) {
      const selectedItems = this.selectedCheckItems[ip] || [];
      const items = this.checkItemsMap[ip] || [];
      return selectedItems.some(itemId => {
        const item = items.find(i => i.id === itemId);
        return item && item.status === 'FAILED';
      });
    },

    // 修复选中的检查项
    async fixSelectedItems(ip) {
      const selectedItems = this.selectedCheckItems[ip] || [];
      if (selectedItems.length === 0) {
        this.$message.warning('请选择要修复的检查项');
        return;
      }

      try {
        const res = await this.$axiosPost(global.API.fixSelectedCheckItems, {
          clusterId: this.clusterId,
          ip: ip,
          itemIds: selectedItems.join(',')
        });

        if (res.code === 200) {
          this.$message.success('修复指令已发送');
          // 清空选择
          this.$set(this.selectedCheckItems, ip, []);
          // 通过轮询获取最新状态
          this.pollingSearch();
        }
      } catch (error) {
        // 异常情况的日志记录，但不显示错误弹窗
        console.error('修复选中检查项失败:', error);
      }
    },

    /**
     * 检查是否有可重试的已选中项
     */
    hasRetryableSelectedItems(ip) {
      const selectedItems = this.selectedCheckItems[ip] || [];
      const items = this.checkItemsMap[ip] || [];

      if (selectedItems.length === 0) {
        return false;
      }

      return selectedItems.some(itemId => {
        const item = items.find(i => i.id === itemId);
        // 允许重试所有非CHECKING和非TERMINATING状态的检查项
        return item && item.status !== 'CHECKING' && item.status !== 'TERMINATING'
            && (item.status === 'FAILED' || item.status === 'SUCCESS' || item.status === 'SKIPPED');
      });
    },

    /**
     * 停止主机检查
     */
    stopCheck(row) {
      if (!row || !row.ip) {
        this.$message.error('参数错误：主机IP不能为空');
        return;
      }

      try {
        // 立即将该主机所有正在检查的项状态更新为"终止中"
        const items = this.checkItemsMap[row.ip] || [];
        let hasUpdated = false;

        if (items && items.length > 0) {
          items.forEach(item => {
            if (item.status === 'CHECKING') {
              item.status = 'TERMINATING';
              item.message = '正在终止检查...';
              hasUpdated = true;
            }
          });

          if (hasUpdated) {
            // 更新本地缓存，确保UI立即显示变化
            this.$set(this.checkItemsMap, row.ip, [...items]);
          }
        }

        // 调用后端API
        this.$axiosPost(global.API.stopHostCheck, {
          clusterId: this.clusterId,
          ip: row.ip
        }).then(res => {
          if (res && res.code === 200) {
            this.$message.success('已终止主机检查');

            // 延迟1秒后刷新列表，获取最新状态
            setTimeout(() => {
              this.refreshHostList();
            }, 1000);
          } else {
            this.$message.error(res.msg || '终止检查失败');
            this.refreshHostList(); // 还是需要刷新，恢复状态
          }
        }).catch(error => {
          console.error('终止主机检查失败:', error);
          this.$message.error('终止主机检查失败，请检查网络连接');
          this.refreshHostList(); // 出错时也刷新，恢复状态
        });
      } catch (error) {
        console.error('终止主机检查异常:', error);
        this.$message.error('终止主机检查出现异常');
        this.refreshHostList();
      }
    },

    /**
     * 重试主机检查
     */
    retryCheck(row) {
      this.$axiosPost(global.API.rehostCheck, {
        clusterId: this.clusterId,
        ips: row.ip
      }).then(res => {
        if (res.code === 200) {
          this.$message.success('已重新开始检查');
          this.refreshHostList();
        } else {
          this.$message.error(res.msg || '重新检查失败');
        }
      });
    },

    /**
     * 刷新主机列表
     */
    refreshHostList() {
      // 刷新主机列表数据
      this.getEnvironmentList();
    },

    /**
     * 检查所有主机
     */
    checkAllHosts() {
    },

    // 五秒去刷一下
    pollingSearch() {
      this.getEnvironmentList(); // 先立马刷一次
      let self = this;
      if (self.timer) clearInterval(self.timer);
      self.timer = setInterval(() => {
        self.getEnvironmentList(true);
      }, 1000);
    },

    // 查看日志
    viewItemLog(ip, itemId, itemName) {
      // 保存当前选择的检查项信息用于AppleLogViewer组件
      this.currentLogIp = ip;
      this.currentLogItemId = itemId;
      this.currentLogItemName = itemName;
      this.logModalTitle = `日志 - 主机: ${ip}, 检查项: ${itemName}`;
      
      // 打开日志弹窗
      this.logVisible = true;
    },

    // 关闭日志查看弹窗
    closeLogModal() {
      this.logVisible = false;
      this.currentLogIp = null;
      this.currentLogItemId = null;
      this.currentLogItemName = null;
      this.checkItem = null;
    },

    // 不再需要旧的日志类型变化处理方法，使用AppleLogViewer组件了
    handleLogTypeChange() {
      // 该方法保留但不做任何处理，防止兼容问题
    },

    // 不再需要旧的筛选变化处理方法，使用AppleLogViewer组件了
    handleFilterChange(filterData) {
      // 该方法保留但不做任何处理，防止兼容问题
    },

    // 去除HTML标签的辅助函数
    stripHtml(html) {
      if (!html) return '';
      const tmp = document.createElement('DIV');
      tmp.innerHTML = html;
      return tmp.textContent || tmp.innerText || '';
    },

    // 找到渲染检查项状态的方法或函数，添加对FIXING状态的处理
    getStatusText(status) {
      if (!status) return '未知';

      const statusMap = {
        'SUCCESS': '成功',
        'FAILED': '失败',
        'CHECKING': '检查中',
        'FIXING': '修复中', // 添加对FIXING状态的处理
        'WAITING': '等待检查'
      };

      return statusMap[status] || '未知';
    },

    // 找到设置状态样式的方法，添加FIXING状态的样式
    getStatusStyle(status) {
      if (!status) return '';

      const styleMap = {
        'SUCCESS': 'success-status',
        'FAILED': 'failed-status',
        'CHECKING': 'checking-status',
        'FIXING': 'fixing-status', // 添加FIXING状态的样式
        'WAITING': 'waiting-status'
      };

      return styleMap[status] || '';
    },

    /**
     * 更新检查状态
     * 判断是否有主机正在进行检查，更新按钮状态
     */
    updateCheckingStatus(hostList) {
      if (!hostList || hostList.length === 0) {
        this.isCheckingActive = false;
        return;
      }

      // 判断是否有主机正在检查
      const hasCheckingHost = hostList.some(host => {
        // 检查主机状态
        if (host.status === 'CHECKING' || host.statusStr === 'CHECKING') {
          return true;
        }

        // 检查所有检查项状态
        const checkItems = host.checkItems || [];
        return checkItems.some(item => item.status === 'CHECKING');
      });

      this.isCheckingActive = hasCheckingHost;
    },

    /**
     * 处理检查操作
     * 根据当前状态调用开始检查或重试检查
     */
    async handleCheckAction() {
      if (this.isCheckingActive) {
        // 当前正在检查中，执行终止操作
        await this.stopHostCheck();
      } else if (this.hasStartedCheck) {
        // 如果已经开始过检查，则调用重试
        this.retryEnvironment('all');
      } else {
        // 首次开始检查
        await this.startHostCheck();
        // 标记已开始过检查
        this.hasStartedCheck = true;
      }
    },

    // 添加方法到methods部分
    showHostsFilePreview(row) {
      // 创建hosts文件预览弹窗
      this.$confirm({
        title: `${row.hostname} 的hosts文件`,
        icon: h => h('a-icon', { props: { type: 'file-text', theme: 'twoTone', twoToneColor: '#34C759' } }),
        content: h => {
          const hostsContent = row.hostsFile || '暂无hosts文件内容';
          return h('div', {
            style: {
              maxHeight: '400px',
              overflow: 'auto',
              padding: '16px',
              backgroundColor: '#F5F5F7',
              borderRadius: '8px',
              fontFamily: 'monospace',
              whiteSpace: 'pre-wrap',
              fontSize: '13px',
              lineHeight: '1.6'
            }
          }, [hostsContent]);
        },
        width: 600,
        okText: '关闭',
        cancelText: null,
        okType: 'default',
        class: 'hosts-file-preview-modal'
      });
    },

    // 打开编辑主机名对话框
    editHostname(record) {
      this.currentEditHost = record;
      this.newHostname = record.hostname || '';
      this.hostnameEditVisible = true;
      
      // 在下一个DOM更新循环后，聚焦输入框
      this.$nextTick(() => {
        if (this.$refs.hostnameInput) {
          this.$refs.hostnameInput.focus();
          this.$refs.hostnameInput.select();
        }
      });
    },
    
    // 提交主机名修改
    submitHostnameEdit() {
      if (!this.newHostname) {
        this.$message.error('主机名不能为空');
        return;
      }
      
      this.editLoading = true;
      
      // 调用后端接口修改主机名
      this.$http.post('/host/updateHostname', {
        clusterId: this.currentEditHost.clusterId,
        ip: this.currentEditHost.ip,
        hostname: this.newHostname
      }).then(res => {
        if (res.code === 200) {
          this.$message.success('主机名修改成功');
          // 更新本地数据
          this.currentEditHost.hostname = this.newHostname;
          // 关闭对话框
          this.hostnameEditVisible = false;
        } else {
          this.$message.error(res.msg || '主机名修改失败');
        }
      }).catch(err => {
        this.$message.error('主机名修改失败: ' + (err.message || err));
      }).finally(() => {
        this.editLoading = false;
      });
    },
    
    // 取消编辑
    cancelHostnameEdit() {
      this.hostnameEditVisible = false;
      this.currentEditHost = null;
      this.newHostname = '';
    },
  },
  mounted() {
    // 重置初始状态
    this.hasStartedCheck = false;

    // 直接开始轮询
    this.pollingSearch();

    // 注册新的统一日志API
    if (!global.API.getLog) {
      global.API.getLog = '/ddh/host/check/getLog';
    }

    // 添加复制功能到window对象
    window.copyToClipboard = function(text) {
      // 声明toast变量在函数最外层作用域
      var toast;
      var toastContent = '';
      var toastStyle = '';

      // 直接实现复制功能，不依赖navigator.clipboard
      try {
        // 创建一个隐藏的textarea元素
        var textarea = document.createElement('textarea');
        textarea.value = text;
        // 设置样式使其不可见
        textarea.style.position = 'fixed';
        textarea.style.top = '-100vh';
        textarea.style.left = '-100vw';
        textarea.style.opacity = '0';

        // 添加到DOM
        document.body.appendChild(textarea);

        // 选择文本
        textarea.select();
        textarea.setSelectionRange(0, 99999); // 兼容移动设备

        // 执行复制
        var successful = document.execCommand('copy');

        // 移除元素
        document.body.removeChild(textarea);

        // 显示苹果风格的提示
        if (successful) {
          // 成功提示内容
          toastContent = '<div style="display: flex; align-items: center;"><svg viewBox="0 0 24 24" fill="none" width="20" height="20" style="margin-right: 8px;"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" fill="currentColor"></path></svg><span>复制成功</span></div>';
          toastStyle = 'position: fixed; top: 20px; left: 50%; transform: translateX(-50%); background-color: rgba(0, 0, 0, 0.75); color: white; padding: 10px 16px; border-radius: 8px; font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif; font-size: 14px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); z-index: 9999; opacity: 0; transition: opacity 0.2s ease-in-out;';
        } else {
          // 失败提示内容
          toastContent = '<div style="display: flex; align-items: center;"><svg viewBox="0 0 24 24" fill="none" width="20" height="20" style="margin-right: 8px;"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" fill="currentColor"></path></svg><span>复制失败</span></div>';
          toastStyle = 'position: fixed; top: 20px; left: 50%; transform: translateX(-50%); background-color: rgba(255, 59, 48, 0.9); color: white; padding: 10px 16px; border-radius: 8px; font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif; font-size: 14px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); z-index: 9999; opacity: 0; transition: opacity 0.2s ease-in-out;';
        }

      } catch (err) {
        console.error('复制失败:', err);

        // 错误提示内容
        toastContent = '<div style="display: flex; align-items: center;"><svg viewBox="0 0 24 24" fill="none" width="20" height="20" style="margin-right: 8px;"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" fill="currentColor"></path></svg><span>复制失败</span></div>';
        toastStyle = 'position: fixed; top: 20px; left: 50%; transform: translateX(-50%); background-color: rgba(255, 59, 48, 0.9); color: white; padding: 10px 16px; border-radius: 8px; font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif; font-size: 14px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); z-index: 9999; opacity: 0; transition: opacity 0.2s ease-in-out;';
      }

      // 创建并显示toast提示
      toast = document.createElement('div');
      toast.innerHTML = toastContent;
      toast.style.cssText = toastStyle;

      // 添加到DOM
      document.body.appendChild(toast);

      // 显示动画
      setTimeout(function() {
        toast.style.opacity = '1';
      }, 10);

      // 自动消失
      setTimeout(function() {
        toast.style.opacity = '0';
        setTimeout(function() {
          if (toast && toast.parentNode) {
            document.body.removeChild(toast);
          }
        }, 300);
      }, 2000);
    };
  },
  beforeDestroy() {
    // 清理定时器
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }

    // 清理日志刷新定时器
    this.stopAutoRefresh();

    // 清理window对象上的方法
    window.copyToClipboard = undefined;
  },
};
</script>

<style lang="less" scoped>
// 苹果设计系统颜色
@apple-white: #ffffff;
@apple-black: #1d1d1f;
@apple-gray-light: #f5f5f7;
@apple-gray: #86868b;
@apple-blue: #0071e3;
@apple-blue-hover: #147CE5;
@apple-red: #ff453a;
@apple-green: #30d158;
@apple-yellow: #ffd60a;
@apple-orange: #ff9f0a;

// 苹果设计系统字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.steps2 {
  margin: 0;
  max-width: 100%;
  background-color: @apple-white;
  overflow: hidden;
  animation: fadeIn 0.8s ease-out;

  .hero-section {
    text-align: center;
    margin-bottom: 2.5rem;
    position: relative;

    .hero-title {
      .apple-font();
      font-size: 2.5rem;
      font-weight: 600;
      line-height: 1.1;
      letter-spacing: -0.022em;
      color: @apple-black;
      margin-bottom: 0.8rem;
      background: linear-gradient(120deg, @apple-black, #505050);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .hero-subtitle {
      .apple-font();
      font-size: 1.2rem;
      line-height: 1.4;
      letter-spacing: 0;
      font-weight: 400;
      color: @apple-gray;
      margin: 0 auto 1.5rem;
      max-width: 600px;
    }

    .queue-status-area {
      display: flex;
      justify-content: center;
      margin-top: 1rem;
    }
  }

  .hosts-table-container {
    border-radius: 12px;
    margin: 0 auto;
    max-width: 1400px;
    overflow: hidden;
    animation: slideUp 0.6s ease-out;
    animation-fill-mode: both;
    animation-delay: 0.2s;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);

    // 表格通用设置
    /deep/ .ant-table {
      .apple-font();

      .ant-table-thead > tr > th {
        background-color: @apple-gray-light;
        font-weight: 600;
        font-size: 0.95rem;
        color: @apple-black;
        padding: 16px 20px;
        border-bottom: 1px solid rgba(0,0,0,0.05);
        white-space: nowrap; // 防止列标题换行
        text-align: center; // 表头文字居中
      }

      .ant-table-tbody > tr > td {
        padding: 14px 20px;
        border-bottom: 1px solid rgba(0,0,0,0.03);
        transition: background-color 0.3s;
      }

      .ant-table-tbody > tr:hover:not(.ant-table-expanded-row):not(.ant-table-row-selected) > td {
        background-color: fadeout(@apple-gray-light, 50%);
      }

      .ant-table-tbody > tr.ant-table-expanded-row > td,
      .ant-table-tbody > tr.ant-table-expanded-row:hover > td {
        background-color: @apple-gray-light;
        padding: 0;
      }

      // 设置扩展行内部的样式
      .ant-table-expanded-row {
        .check-items-container {
          padding: 1.5rem 2rem 1.5rem 3rem;

          .check-items-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.2rem;

            .header-summary {
              .apple-font();
              font-size: 1.1rem;
              font-weight: 500;
              color: @apple-black;

              .status-icon {
                margin-right: 8px;
              }

              .success-count {
                color: @apple-green;
                margin: 0 4px;
              }

              .failed-count {
                color: @apple-red;
                margin: 0 4px;
              }
            }

            .header-actions {
              display: flex;
              gap: 10px;

              .apple-button {
                height: 34px;
                padding: 0 16px;
                font-size: 14px;
                font-weight: 500;
                border-radius: 17px;
                transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

                &.primary {
                  background: @apple-blue;
                  border: none;
                  color: white;

                  &:hover {
                    background: @apple-blue-hover;
                  }
                }

                &.secondary {
                  background: @apple-gray-light;
                  border: none;
                  color: @apple-black;

                  &:hover {
                    background: darken(@apple-gray-light, 5%);
                  }
                }

                &.danger {
                  background: fade(@apple-red, 10%);
                  border: none;
                  color: @apple-red;

                  &:hover {
                    background: fade(@apple-red, 15%);
                  }
                }
              }
            }
          }

          // 子表格样式
          .ant-table {
            border-radius: 12px;
            overflow: hidden;

            .ant-table-thead > tr > th {
              background-color: rgba(0,0,0,0.02);
              font-weight: 500;
              font-size: 0.9rem;
              color: @apple-black;
              padding: 12px 16px;
            }

            .ant-table-tbody > tr > td {
              padding: 12px 16px;
              font-size: 0.9rem;
            }

            // 优化内嵌表格的选择框样式
            .ant-checkbox-wrapper {
              .ant-checkbox-inner {
                border-radius: 4px;
                border-color: @apple-gray;

                &:after {
                  border-color: white;
                }
              }

              .ant-checkbox-checked .ant-checkbox-inner {
                background-color: @apple-blue;
                border-color: @apple-blue;
              }
            }

            // 状态标签样式
            .status-tag {
              display: inline-flex;
              align-items: center;
              padding: 4px 12px;
              border-radius: 12px;
              font-size: 0.85rem;
              font-weight: 500;

              .status-icon {
                margin-right: 6px;
              }

              &.success {
                background-color: fade(@apple-green, 10%);
                color: @apple-green;
              }

              &.failed {
                background-color: fade(@apple-red, 10%);
                color: @apple-red;
              }

              &.waiting {
                background-color: fade(@apple-yellow, 10%);
                color: darken(@apple-yellow, 15%);
              }

              &.checking {
                background-color: fade(@apple-blue, 10%);
                color: @apple-blue;
              }

              &.skipped {
                background-color: fade(@apple-gray, 10%);
                color: @apple-gray;
              }
            }

            // 操作按钮样式
            .action-button {
              height: 28px;
              padding: 0 12px;
              font-size: 0.85rem;
              font-weight: 500;
              border-radius: 14px;
              transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
              margin-right: 8px;

              &.view {
                background: fade(@apple-blue, 10%);
                border: none;
                color: @apple-blue;

                &:hover {
                  background: fade(@apple-blue, 15%);
                }
              }

              &.fix {
                background: fade(@apple-red, 10%);
                border: none;
                color: @apple-red;

                &:hover {
                  background: fade(@apple-red, 15%);
                }
              }

              &.retry {
                background: fade(@apple-orange, 10%);
                border: none;
                color: @apple-orange;

                &:hover {
                  background: fade(@apple-orange, 15%);
                }
              }

              &.skip {
                background: fade(@apple-gray, 10%);
                border: none;
                color: @apple-gray;

                &:hover {
                  background: fade(@apple-gray, 15%);
                }
              }
            }
          }
        }
      }
    }
  }

  // 自定义展开图标样式
  /deep/ .apple-expand-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border-radius: 12px;
    background-color: @apple-gray-light;
    cursor: pointer;
    transition: background-color 0.3s;

    &:hover {
      background-color: darken(@apple-gray-light, 3%);
    }

    .expand-icon-inner {
      display: flex;
      align-items: center;
      justify-content: center;

      .anticon {
        color: @apple-black;
        transform: rotate(0);
      }
    }

    &.expanded {
      background-color: @apple-blue;

      .anticon {
        color: white;
        transform: rotate(90deg);
      }
    }
  }

  // 主机列表操作按钮样式
  .apple-actions {
    display: flex;
    gap: 8px;

    .apple-button {
      height: 32px;
      padding: 0 16px;
      font-size: 14px;
      font-weight: 500;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;

      &.primary {
        background: @apple-blue;
        border: none;
        color: white;

        &:hover {
          background: darken(@apple-blue, 5%);
          transform: translateY(-1px);
          box-shadow: 0 2px 6px rgba(0, 113, 227, 0.3);
        }

        &:active {
          transform: translateY(0);
        }
      }

      &.danger {
        background: fade(@apple-red, 10%);
        border: none;
        color: @apple-red;

        &:hover {
          background: fade(@apple-red, 15%);
          transform: translateY(-1px);
          box-shadow: 0 2px 6px rgba(255, 69, 58, 0.2);
        }

        &:active {
          transform: translateY(0);
        }
      }
    }
  }

  // 操作系统信息样式
  .os-info {
    .os-type {
      font-size: 0.95rem;
      color: @apple-black;

      .anticon {
        font-size: 16px;
      }

      .os-logo {
        width: 20px;
        height: 20px;
        object-fit: contain;
        vertical-align: middle;
        transition: transform 0.3s ease;
      }

      &:hover .os-logo {
        transform: scale(1.1);
      }
    }
  }

  // 操作系统详情弹出框样式
  .os-detail-popup {
    padding: 0;
    min-width: 320px;
    max-width: 420px;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
    background-color: #ffffff;
    animation: osFadeIn 0.3s ease-in-out;
    max-height: 80vh;
    overflow-y: auto;

    // 滚动条样式
    &::-webkit-scrollbar {
      width: 8px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background-color: rgba(0, 0, 0, 0.1);
      border-radius: 8px;
      border: 2px solid transparent;
      background-clip: content-box;
    }

    &::-webkit-scrollbar-thumb:hover {
      background-color: rgba(0, 0, 0, 0.2);
      border: 2px solid transparent;
      background-clip: content-box;
    }

    // 头部区域
    .os-detail-header {
      padding: 24px;
      display: flex;
      align-items: center;
      background: linear-gradient(135deg, #0066CC, #007AFF);

      .os-detail-icon-container {
        margin-right: 20px;

        img {
          background-color: #ffffff;
          padding: 6px;
          border-radius: 12px;
          box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
        }
      }

      .os-detail-title-container {
        color: #ffffff;

        .os-detail-title {
          font-size: 1.3rem;
          font-weight: 600;
          margin: 0 0 4px 0;
        }

        .os-detail-subtitle {
          font-size: 0.9rem;
          opacity: 0.9;
          margin-bottom: 2px;
        }
      }
    }

    // 卡片样式
    .os-detail-card {
      margin: 16px;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      background-color: #ffffff;
      border: 1px solid rgba(0, 0, 0, 0.05);

      &:last-child {
        margin-bottom: 16px;
      }

      .os-detail-card-header {
        padding: 12px 16px;
        font-size: 0.95rem;
        font-weight: 600;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
        background-color: rgba(0, 0, 0, 0.02);
      }

      // 信息行
      .os-detail-info-row {
        display: flex;
        padding: 16px;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);

        &:last-child {
          border-bottom: none;
        }

        .os-detail-info-icon-container {
          margin-right: 16px;

          .os-detail-info-icon {
            width: 36px;
            height: 36px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;

            i {
              font-size: 20px;
              color: #ffffff;
            }

            &.cpu {
              background: linear-gradient(135deg, #FF2D55, #FF375F);
            }

            &.memory {
              background: linear-gradient(135deg, #007AFF, #0A84FF);
            }

            &.storage {
              background: linear-gradient(135deg, #5856D6, #5E5CE6);
            }

            &.swap {
              background: linear-gradient(135deg, #34C759, #30D158);
            }

            &.gpu {
              background: linear-gradient(135deg, #FF9500, #FF9F0A);
            }
          }
        }

        .os-detail-info-content {
          flex: 1;

          .os-detail-info-label {
            font-size: 0.8rem;
            color: #8E8E93;
            margin-bottom: 6px;
          }

          .os-detail-info-value {
            font-size: 1rem;
            font-weight: 600;
            color: #1d1d1f;
            margin-bottom: 6px;

            &.with-progress {
              display: flex;
              justify-content: space-between;
              align-items: center;

              span {
                margin-right: 10px;
              }
            }
          }

          .os-detail-progress-container {
            flex: 1;
            height: 6px;
            background-color: rgba(0, 0, 0, 0.05);
            border-radius: 3px;
            overflow: hidden;

            .os-detail-progress-bar {
              height: 100%;
              border-radius: 3px;
              transition: width 0.3s ease;
            }
          }

          .os-detail-info-subvalue {
            font-size: 0.8rem;
            color: #8E8E93;
          }
        }
      }

      // 表格样式
      .os-detail-table {
        .os-detail-table-row {
          display: flex;
          padding: 12px 16px;
          border-bottom: 1px solid rgba(0, 0, 0, 0.05);

          &:last-child {
            border-bottom: none;
          }

          &:hover {
            background-color: rgba(0, 0, 0, 0.02);
          }

          .os-detail-table-cell {
            &.label {
              width: 110px;
              color: #8E8E93;
              font-size: 0.9rem;
            }

            &.value {
              flex: 1;
              font-weight: 500;
              color: #1d1d1f;
              font-size: 0.9rem;
            }
          }
        }
      }
    }

    // 底部区域
    .os-detail-footer {
      padding: 12px 16px;
      background-color: rgba(0, 0, 0, 0.02);
      border-top: 1px solid rgba(0, 0, 0, 0.05);
      display: flex;
      justify-content: space-between;
      font-size: 0.8rem;
      color: #8E8E93;
    }

    // 无信息提示
    .os-detail-no-info {
      padding: 32px;
      text-align: center;

      .os-detail-no-info-text {
        font-size: 1rem;
        font-weight: 500;
        margin-bottom: 8px;
        color: #1d1d1f;
      }

      .os-detail-no-info-subtext {
        font-size: 0.85rem;
        color: #8E8E93;
      }
    }
  }

  @keyframes osFadeIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
}

// 苹果风格模态框样式
:global(.apple-modal) {
  .ant-modal-content {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  }

  .ant-modal-header {
    padding: 20px 24px;
    background: @apple-white;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);

    .ant-modal-title {
      .apple-font();
      font-size: 1.2rem;
      font-weight: 500;
      color: @apple-black;
    }
  }

  .ant-modal-body {
    padding: 24px;
  }

  .ant-modal-footer {
    border-top: 1px solid rgba(0, 0, 0, 0.05);
    padding: 16px 24px;

    .ant-btn {
      height: 36px;
      padding: 0 18px;
      font-size: 0.95rem;
      font-weight: 500;
      border-radius: 18px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

      &.ant-btn-primary {
        background: @apple-blue;
        border: none;

        &:hover {
          background: @apple-blue-hover;
        }
      }

      &.ant-btn-dangerous {
        background: @apple-red;
        border: none;
        color: white;

        &:hover {
          background: darken(@apple-red, 5%);
        }
      }

      &:not(.ant-btn-primary):not(.ant-btn-dangerous) {
        background: @apple-gray-light;
        border: none;
        color: @apple-black;

        &:hover {
          background: darken(@apple-gray-light, 5%);
        }
      }
    }
  }
}

// 动画
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}



// 添加Apple风格表格的CSS样式到样式部分
.apple-style-table {
  font-family: "SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif;

  /deep/ .ant-table-thead > tr > th {
    background-color: rgba(0, 0, 0, 0.02);
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    color: #1d1d1f;
    font-weight: 600;
    padding: 16px;
    transition: background 0.2s ease;
  }

  /deep/ .ant-table-tbody > tr > td {
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    padding: 16px;
    transition: background 0.2s ease;
  }

  /deep/ .ant-table-tbody > tr:hover > td {
    background-color: rgba(0, 0, 0, 0.02);
  }

  /deep/ .ant-table-row-selected > td {
    background-color: rgba(0, 122, 255, 0.05) !important;
  }

  /deep/ .ant-checkbox-checked .ant-checkbox-inner {
    background-color: #007AFF;
    border-color: #007AFF;
  }

  /deep/ .ant-checkbox-wrapper:hover .ant-checkbox-inner,
  /deep/ .ant-checkbox:hover .ant-checkbox-inner {
    border-color: #007AFF;
  }
}

.check-items-container {
  animation: fadeInAndSlideUp 0.5s ease forwards;
}

@keyframes fadeInAndSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.check-result-tooltip {
  max-height: 80vh;
  overflow-y: hidden;
  border-radius: 16px !important;
  box-shadow: none !important;
  border: none !important;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.03);
    border-radius: 8px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 8px;

    &:hover {
      background: rgba(0, 0, 0, 0.2);
    }
  }
}

/* 强制覆盖样式 */
/deep/ .ant-tooltip.apple-style-tooltip {
  .ant-tooltip-content,
  .ant-tooltip-arrow,
  .ant-tooltip-inner {
    background: transparent !important;
    background-color: transparent !important;
    border: none !important;
    box-shadow: none !important;
  }

  .ant-tooltip-arrow::before {
    background: transparent !important;
    background-color: transparent !important;
    border: none !important;
    box-shadow: none !important;
  }
}

// 修复按钮样式
.action-buttons button {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.action-buttons button:hover {
  filter: brightness(1.05);
}

.action-buttons button:active {
  filter: brightness(0.95);
  transform: translateY(1px);
}

.hostname-skeleton {
  width: 120px;
  height: 18px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

.os-info-loading {
  position: relative;
  height: 24px;
  width: 90%;
  border-radius: 4px;
  background-color: rgba(240, 240, 240, 0.8);
  overflow: hidden;
}

.os-info-loading::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 30%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.8), transparent);
  animation: shine 1.5s infinite;
}

@keyframes shine {
  0% { transform: translateX(0); }
  100% { transform: translateX(500%); }
}

/* 修改操作系统详情弹出框加载动画 */
.os-detail-loading {
  padding: 0;
  min-width: 320px;
  max-width: 420px;
  min-height: 200px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  animation: osFadeIn 0.3s ease-in-out;
  display: flex;
  flex-direction: column;
}

.os-detail-loading-header {
  height: 100px;
  background: linear-gradient(135deg, #f0f0f0, #e0e0e0);
  animation: pulse 1.5s infinite ease-in-out;
}

.os-detail-loading-content {
  padding: 16px;
  flex: 1;
}

.os-detail-loading-line {
  height: 12px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  border-radius: 4px;
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.os-detail-loading-line.short {
  width: 70%;
}

.os-detail-loading-line.medium {
  width: 85%;
}

.hostname-detail-loading {
  padding: 0;
  min-width: 320px;
  max-width: 420px;
  min-height: 200px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  animation: osFadeIn 0.3s ease-in-out;
  display: flex;
  flex-direction: column;
}

.hostname-detail-loading-header {
  height: 100px;
  background: linear-gradient(135deg, #f0f0f0, #e0e0e0);
  animation: pulse 1.5s infinite ease-in-out;
}

.hostname-detail-loading-content {
  padding: 16px;
  flex: 1;
}

.hostname-detail-loading-line {
  height: 12px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  border-radius: 4px;
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.hostname-detail-loading-line.short {
  width: 70%;
}

.hostname-detail-loading-line.medium {
  width: 85%;
}

.hostname-detail-loading-text {
  font-size: 14px;
  color: #007AFF;
  text-align: center;
  margin-top: 12px;
  font-weight: 500;
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

@keyframes shimmer {
  to {
    background-position: -100% 0;
  }
}

/* 为操作系统详情弹出框添加动画 */
@keyframes osFadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.steps-container {
  // 添加硬件信息收集状态的动画和样式
  .os-detail-info-value.loading {
    display: flex;
    align-items: center;
    color: #8E8E93;
  }

  .loading-animation {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(0, 122, 255, 0.1);
    border-top-color: rgba(0, 122, 255, 0.8);
    border-radius: 50%;
    margin-right: 8px;
    animation: spin 1s linear infinite;
  }

  .loading-text {
    font-size: 14px;
    color: #8E8E93;
  }

  .loading-text-simple {
    font-size: 14px;
    color: #8E8E93;
    font-style: italic;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  // OS信息加载动画
  .os-info-loading {
    position: relative;
    overflow: hidden;

    &:after {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 50%;
      height: 100%;
      background: linear-gradient(90deg,
      rgba(255, 255, 255, 0) 0%,
      rgba(255, 255, 255, 0.3) 50%,
      rgba(255, 255, 255, 0) 100%);
      animation: shimmer 1.5s infinite;
    }
  }

  @keyframes shimmer {
    to {
      left: 100%;
    }
  }

  @keyframes osFadeIn {
    from {
      opacity: 0;
      transform: translateY(4px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
}

.gpu-memory-info {
  margin-top: 4px;
  font-size: 12px;
  color: #8e8e93;
  line-height: 1.2;
  display: inline-block;
  padding: 2px 6px;
  background-color: #f2f2f7;
  border-radius: 4px;
}

.table-operations {
  .operation-group {
    display: flex;
    gap: 12px;
    align-items: center;

    .apple-batch-button {
      height: 40px;
      padding: 0 20px;
      border: none;
      background: linear-gradient(180deg, #0A84FF 0%, #0066CC 100%);
      border-radius: 20px;
      color: white;
      font-size: 14px;
      font-weight: 500;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      box-shadow: 0 2px 6px rgba(0, 122, 255, 0.2);

      .anticon {
        font-size: 14px;
        margin-right: 6px;
      }

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        background: linear-gradient(180deg, #0091FF 0%, #0077ED 100%);
        box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
      }

      &:active:not(:disabled) {
        transform: translateY(0);
        background: linear-gradient(180deg, #0077ED 0%, #0066CC 100%);
        box-shadow: 0 2px 6px rgba(0, 122, 255, 0.2);
      }

      &:disabled {
        background: linear-gradient(180deg, #E5E5EA 0%, #D1D1D6 100%);
        color: #8E8E93;
        box-shadow: none;
        cursor: not-allowed;

        .anticon {
          opacity: 0.5;
        }
      }
    }
  }
}

// 添加苹果风格按钮样式
.apple-button {
  height: 36px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  .anticon {
    margin-right: 6px;
    font-size: 16px;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }

  &:active {
    transform: translateY(1px);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  }

  &:disabled {
    background-color: #f5f5f7;
    color: rgba(0, 0, 0, 0.25);
    border-color: #d9d9d9;
    box-shadow: none;

    &:hover {
      transform: none;
      box-shadow: none;
    }
  }
}

.apple-primary-button {
  background: linear-gradient(135deg, #0a84ff, #0066ff);
  border: none;
  color: white;

  &:hover {
    background: linear-gradient(135deg, #1d90ff, #0070ff);
  }

  &:active {
    background: linear-gradient(135deg, #0070e0, #0060e0);
  }
}

.apple-danger-button {
  background: linear-gradient(135deg, #ff453a, #ff3b30);
  border: none;
  color: white;

  &:hover {
    background: linear-gradient(135deg, #ff5147, #ff4b40);
  }

  &:active {
    background: linear-gradient(135deg, #e03a30, #d0362c);
  }
}

.apple-batch-button {
  background: #f5f5f7;
  border: 1px solid #e0e0e5;
  color: #1d1d1f;

  &:hover {
    background: #f8f8fa;
    border-color: #d9d9df;
  }

  &:active {
    background: #eaeaed;
    border-color: #d0d0d5;
  }
}

// 表格操作区域样式
.table-operations {
  padding: 8px 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  z-index: 1;

  .operation-group {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.hosts-file-tooltip {
  max-height: 400px;
  max-width: 600px;
  overflow: auto;
  margin: 0;
  padding: 10px;
  background-color: #f8f8f8;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.hosts-detail-loading {
  padding: 0;
  min-width: 320px;
  max-width: 420px;
  min-height: 200px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  animation: osFadeIn 0.3s ease-in-out;
  display: flex;
  flex-direction: column;
}

.hosts-detail-loading-header {
  height: 100px;
  background: linear-gradient(135deg, #f0f0f0, #e0e0e0);
  animation: pulse 1.5s infinite ease-in-out;
}

.hosts-detail-loading-content {
  padding: 16px;
  flex: 1;
}

.hosts-detail-loading-line {
  height: 12px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  border-radius: 4px;
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.hosts-detail-loading-line.short {
  width: 70%;
}

.hosts-detail-loading-line.medium {
  width: 85%;
}

.hosts-detail-loading-line.long {
  width: 100%;
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

@keyframes shimmer {
  to {
    background-position: -100% 0;
  }
}

.hosts-detail-loading-text {
  font-size: 14px;
  color: #007AFF;
  text-align: center;
  margin-top: 12px;
  font-weight: 500;
}

.hosts-info-loading {
  position: relative;
  height: 24px;
  width: 90%;
  border-radius: 4px;
  background-color: rgba(240, 240, 240, 0.8);
  overflow: hidden;
}

.hosts-info-loading::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 30%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.8), transparent);
  animation: shine 1.5s infinite;
}

@keyframes shine {
  to { transform: translateX(500%); }
}

.hosts-file-tooltip {
  max-width: 650px;

  pre {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

// 文件图标容器悬停效果
.file-icon-container {
  &:hover {
    .anticon {
      color: #0A84FF;
    }
  }
}

// 添加hostname tooltip的样式
.hostname-detail-tooltip {
  min-width: 400px;
  max-width: 500px;
  padding: 16px;
  background: #FFFFFF;
  border-radius: 16px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  font-family: "SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.hostname-detail-section {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.hostname-detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #1D1D1F;
  margin-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding-bottom: 8px;
}

.hostname-detail-subtitle {
  font-size: 14px;
  font-weight: 500;
  color: #1D1D1F;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.hostname-detail-block {
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
}

.hostname-detail-content {
  padding: 0 0 0 8px;
}

.hostname-detail-item {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.hostname-detail-label {
  width: 80px;
  color: #8E8E93;
  flex-shrink: 0;
}

.hostname-detail-value {
  color: #1D1D1F;
  flex: 1;
  word-break: break-all;
}

.hostname-detail-empty {
  color: #8E8E93;
  font-style: italic;
  font-size: 13px;
  padding: 4px 8px;
}

.hostname-detail-loading {
  display: flex;
  align-items: center;
  padding: 8px;
}

.hostname-detail-error {
  color: #FF3B30;
  display: flex;
  align-items: center;
  padding: 8px;
}

.dns-servers {
  font-family: monospace;
  background-color: #F5F5F7;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.hostname-detail-hosts-content {
  width: 100%;
  font-size: 12px;
}

.host-check-table {
  // 调整多选框和展开按钮列的宽度
  /deep/ .ant-table-selection-column {
    width: 30px !important;
    min-width: 30px !important;
    padding-left: 8px !important;
    padding-right: 0 !important;
  }

  /deep/ .ant-table-row-expand-icon-cell {
    width: 30px !important;
    min-width: 30px !important;
    padding-left: 0 !important;
    padding-right: 8px !important;
  }

  // 确保所有列的内容不换行
  /deep/ .ant-table-cell {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 使用新的AppleLogViewer组件，不再需要旧的日志相关样式

/* 主机名和相关DNS/hosts悬浮框样式 */
.hostname-detail-tooltip {
  padding: 16px;
  min-width: 360px;
  max-width: 520px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.hostname-detail-section {
  margin-bottom: 16px;
}

.hostname-detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #e5e5ea;
}

.hostname-detail-content {
  margin-bottom: 12px;
}

.hostname-detail-item {
  display: flex;
  margin-bottom: 6px;
  line-height: 1.4;
}

.hostname-detail-label {
  font-weight: 500;
  color: #8e8e93;
  width: 80px;
  flex-shrink: 0;
}

.hostname-detail-value {
  flex: 1;
  color: #1d1d1f;
}

.hostname-detail-subtitle {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 8px;
  margin-top: 12px;
}

.hostname-detail-block {
  margin-bottom: 16px;
}

.hostname-detail-empty {
  padding: 12px;
  background-color: #f5f5f7;
  border-radius: 8px;
  color: #8e8e93;
  font-style: italic;
  text-align: center;
}

.hostname-detail-loading {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: rgba(0, 122, 255, 0.05);
  border-radius: 8px;
  color: #007aff;
}

.hostname-detail-error {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: rgba(255, 59, 48, 0.05);
  border-radius: 8px;
  color: #ff3b30;
}

.loading-animation {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(0, 122, 255, 0.3);
  border-top-color: #007aff;
  border-radius: 50%;
  margin-right: 8px;
  animation: spin 1s linear infinite;
}

.loading-text {
  font-size: 13px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 操作系统和硬件信息悬浮框样式 */
.os-detail-popup {
  padding: 20px;
  width: 460px;
  max-width: 100%;
  background: #fff;
  border-radius: 16px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.os-detail-header {
  display: flex;
  margin-bottom: 24px;
}

.os-detail-icon-container {
  width: 64px;
  height: 64px;
  margin-right: 16px;
  flex-shrink: 0;
}

.os-detail-title-container {
  flex: 1;
}

.os-detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px 0;
}

.os-detail-subtitle {
  font-size: 14px;
  color: #6e6e73;
  margin: 0 0 4px 0;
}

.os-detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.os-detail-card {
  background: #f5f5f7;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.os-detail-card-header {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.os-detail-info-row {
  display: flex;
  margin-bottom: 12px;
}

.os-detail-info-icon-container {
  margin-right: 12px;
  flex-shrink: 0;
}

.os-detail-info-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background-color: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.os-detail-info-icon.cpu {
  background-color: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.os-detail-info-icon.memory {
  background-color: rgba(88, 86, 214, 0.1);
  color: #5856d6;
}

.os-detail-info-icon.disk {
  background-color: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}

.os-detail-info-icon.swap {
  background-color: rgba(52, 199, 89, 0.1);
  color: #34c759;
}

.os-detail-info-icon.gpu {
  background-color: rgba(175, 82, 222, 0.1);
  color: #af52de;
}

.os-detail-info-content {
  flex: 1;
}

.os-detail-info-label {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
}

.os-detail-info-value {
  font-size: 13px;
  color: #6e6e73;
  display: flex;
  align-items: center;
}

.os-detail-info-value.loading {
  display: flex;
  align-items: center;
  color: #007aff;
  font-size: 13px;
}

.os-detail-info-value.error {
  color: #ff3b30;
}

.os-info {
  .os-type {
    font-size: 0.95rem;
    color: @apple-black;

    .anticon {
      font-size: 16px;
    }

    .os-logo {
      width: 20px;
      height: 20px;
      object-fit: contain;
      vertical-align: middle;
      transition: transform 0.3s ease;
    }

    &:hover .os-logo {
      transform: scale(1.1);
    }
  }
}

.os-info-loading {
  position: relative;
  height: 24px;
  width: 90%;
  border-radius: 4px;
  background-color: rgba(240, 240, 240, 0.8);
  overflow: hidden;
}

.os-info-loading::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 30%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.8), transparent);
  animation: shine 1.5s infinite;
}

@keyframes shine {
  to { transform: translateX(500%); }
}

.os-detail-popup {
  padding: 0;
  min-width: 320px;
  max-width: 420px;
  min-height: 200px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  animation: osFadeIn 0.3s ease-in-out;
  display: flex;
  flex-direction: column;
}

.os-detail-loading-header {
  height: 100px;
  background: linear-gradient(135deg, #f0f0f0, #e0e0e0);
  animation: pulse 1.5s infinite ease-in-out;
}

.os-detail-loading-content {
  padding: 16px;
  flex: 1;
}

.os-detail-loading-line {
  height: 12px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  border-radius: 4px;
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.os-detail-loading-line.short {
  width: 70%;
}

.os-detail-loading-line.medium {
  width: 85%;
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

@keyframes shimmer {
  to {
    background-position: -100% 0;
  }
}

.os-detail-loading-text {
  font-size: 14px;
  color: #007AFF;
  text-align: center;
  margin-top: 12px;
  font-weight: 500;
}

.os-detail-header {
  display: flex;
  margin-bottom: 24px;
}

.os-detail-icon-container {
  width: 64px;
  height: 64px;
  margin-right: 16px;
  flex-shrink: 0;
}

.os-detail-title-container {
  flex: 1;
}

.os-detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px 0;
}

.os-detail-subtitle {
  font-size: 14px;
  color: #6e6e73;
  margin: 0 0 4px 0;
}

.os-detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.os-detail-card {
  background: #f5f5f7;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.os-detail-card-header {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.os-detail-info-row {
  display: flex;
  margin-bottom: 12px;
}

.os-detail-info-icon-container {
  margin-right: 12px;
  flex-shrink: 0;
}

.os-detail-info-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background-color: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.os-detail-info-icon.cpu {
  background-color: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.os-detail-info-icon.memory {
  background-color: rgba(88, 86, 214, 0.1);
  color: #5856d6;
}

.os-detail-info-icon.disk {
  background-color: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}

.os-detail-info-icon.swap {
  background-color: rgba(52, 199, 89, 0.1);
  color: #34c759;
}

.os-detail-info-icon.gpu {
  background-color: rgba(175, 82, 222, 0.1);
  color: #af52de;
}

.os-detail-info-content {
  flex: 1;
}

.os-detail-info-label {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
}

.os-detail-info-value {
  font-size: 13px;
  color: #6e6e73;
  display: flex;
  align-items: center;
}

.os-detail-info-value.loading {
  display: flex;
  align-items: center;
  color: #007aff;
  font-size: 13px;
}

.os-detail-info-value.error {
  color: #ff3b30;
}

.os-detail-info-value.with-progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.os-detail-progress-container {
  flex: 1;
  height: 6px;
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
  overflow: hidden;
}

.os-detail-progress-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.os-detail-info-subvalue {
  font-size: 0.8rem;
  color: #8E8E93;
}

// 添加CSS样式到组件样式部分
.hostname-detail-waiting,
.os-detail-info-value.waiting {
  display: flex;
  align-items: center;
  color: #FAAD14;
  font-size: 12px;
  padding: 6px 0;
}

.os-detail-waiting {
  padding: 16px;
  width: 300px;
  text-align: center;
}

.os-detail-waiting-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 16px;
}

// 更新样式部分
.hostname-tooltip {
  .ant-tooltip-inner {
    padding: 0;
    background: transparent;
    box-shadow: none;
  }

  .ant-tooltip-arrow {
    border-right-color: #ffffff !important;
    display: block !important;

    &::before {
      background-color: #ffffff !important;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }
  }
}

.hostname-detail-tooltip {
  min-width: 400px;
  max-width: 500px;
  padding: 20px;
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif;
  animation: tooltipContentFadeIn 0.2s ease-out;
  will-change: transform, opacity;
}

.hostname-detail-section {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.hostname-detail-title {
  font-size: 15px;
  font-weight: 600;
  color: #1D1D1F;
  margin-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding-bottom: 8px;
  display: flex;
  align-items: center;
}

.hostname-detail-content {
  padding: 0 0 0 8px;
}

.hostname-detail-item {
  display: flex;
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;

  &:last-child {
    margin-bottom: 0;
  }
}

.hostname-detail-label {
  width: 80px;
  color: #8E8E93;
  flex-shrink: 0;
  font-weight: 500;
}

.hostname-detail-value {
  color: #1D1D1F;
  flex: 1;
  word-break: break-all;

  &.dns-servers {
    font-family: "SF Mono", "Menlo", monospace;
    background-color: rgba(0, 122, 255, 0.08);
    padding: 6px 8px;
    border-radius: 6px;
    font-size: 12px;
    line-height: 1.5;
    color: #007AFF;
  }
}

.hostname-detail-hosts-file {
  max-height: 200px;
  overflow: auto;
  background: rgba(0, 0, 0, 0.03);
  padding: 8px;
  border-radius: 8px;
  font-size: 12px;
  font-family: "SF Mono", "Menlo", monospace;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.5;
  margin: 0;
  border-left: 3px solid #FF9500;
  color: #1D1D1F;
  width: 100%;
}

.hostname-detail-empty {
  color: #8E8E93;
  font-style: italic;
  font-size: 13px;
  padding: 8px;
  background-color: rgba(0, 0, 0, 0.03);
  border-radius: 6px;
  text-align: center;
}

.hostname-display {
  &:hover {
    color: #0A84FF;
    text-decoration: underline;

    svg {
      color: #0A84FF;
      opacity: 1;
    }
  }
}

@keyframes tooltipContentFadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hostname-detail-loading {
  min-width: 400px;
  max-width: 500px;
  min-height: 320px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  animation: hostname-fade-in 0.3s ease-in-out;
  display: flex;
  flex-direction: column;
}

.hostname-detail-loading-header {
  height: 60px;
  background: linear-gradient(135deg, #f5f5f7, #e5e5ea);
  animation: hostname-pulse 1.5s infinite ease-in-out;
}

.hostname-detail-loading-content {
  padding: 20px;
  flex: 1;
}

.hostname-detail-loading-skeleton {
  margin-bottom: 16px;
}

.hostname-detail-loading-title {
  height: 18px;
  width: 120px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  border-radius: 4px;
  margin-bottom: 16px;
  background-size: 200% 100%;
  animation: hostname-shimmer 1.5s infinite;
}

.hostname-detail-loading-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hostname-detail-loading-item {
  display: flex;
  align-items: center;
}

.hostname-detail-loading-label {
  width: 70px;
  height: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  border-radius: 4px;
  margin-right: 12px;
  flex-shrink: 0;
  background-size: 200% 100%;
  animation: hostname-shimmer 1.5s infinite;
}

.hostname-detail-loading-value {
  height: 12px;
  flex: 1;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  border-radius: 4px;
  background-size: 200% 100%;
  animation: hostname-shimmer 1.5s infinite;
}

.hostname-detail-loading-value.short {
  width: 40%;
}

.hostname-detail-loading-value.medium {
  width: 70%;
}

.hostname-detail-loading-file {
  height: 80px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  border-radius: 8px;
  background-size: 200% 100%;
  animation: hostname-shimmer 1.5s infinite;
}

.hostname-detail-loading-footer {
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-top: 1px solid #f5f5f7;
}

.hostname-loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;

  span {
    display: inline-block;
    width: 8px;
    height: 8px;
    margin: 0 2px;
    background-color: #007AFF;
    border-radius: 50%;
    animation: hostname-loading-bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }

    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

.hostname-detail-loading-text {
  font-size: 14px;
  color: #007AFF;
  font-weight: 500;
}

.hostname-display-loading {
  position: relative;

  .hostname-loading-pulse {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: #007AFF;
    animation: hostname-pulse 1.5s infinite ease-in-out;
  }
}

// 等待状态样式
.hostname-detail-pending {
  min-width: 300px;
  max-width: 400px;
  min-height: 180px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  animation: hostname-fade-in 0.3s ease-in-out;
  display: flex;
  flex-direction: column;
}

.hostname-detail-pending-content {
  padding: 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.hostname-detail-pending-icon {
  margin-bottom: 16px;
  animation: hostname-pending-pulse 2s infinite ease-in-out;
}

.hostname-detail-pending-title {
  font-size: 16px;
  font-weight: 500;
  color: #FAAD14;
  margin-bottom: 8px;
}

.hostname-detail-pending-subtitle {
  font-size: 13px;
  color: #8E8E93;
}

.hostname-detail-tooltip-container {
  padding: 0;
}

@keyframes hostname-pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

@keyframes hostname-pending-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

@keyframes hostname-shimmer {
  to {
    background-position: -200% 0;
  }
}

@keyframes hostname-fade-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes hostname-loading-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 操作系统信息加载中样式 */
.os-detail-loading {
  width: 100%;
  padding: 20px;
}

.os-detail-loading-header {
  height: 60px;
  background-color: #f5f5f7;
  margin-bottom: 16px;
  border-radius: 12px;
  animation: shimmer 1.5s infinite;
}

.os-detail-loading-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.os-detail-loading-line {
  height: 24px;
  background-color: #f5f5f7;
  border-radius: 6px;
  width: 100%;
  animation: shimmer 1.5s infinite;
}

.os-detail-loading-line.short {
  width: 60%;
}

.os-detail-loading-line.medium {
  width: 80%;
}

.os-detail-loading-text {
  margin-top: 16px;
  text-align: center;
  color: #007AFF;
}

@keyframes shimmer {
  0% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.5;
  }
}

// 操作系统信息加载样式
.os-info-loading {
  .loading-animation {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, rgba(255,255,255,0), rgba(255,255,255,0.6), rgba(255,255,255,0));
    background-size: 200% 100%;
    animation: loading-wave 1.5s infinite;
  }

  @keyframes loading-wave {
    0% {
      background-position: -100% 0;
    }
    100% {
      background-position: 100% 0;
    }
  }
}

// 主机名悬浮卡片样式
.hostname-detail-tooltip {
  padding: 0;
}

.hostname-tooltip {
  max-width: none !important;
}

.hostname-column {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 10px;
  
  .hostname-text {
    flex: 1;
    display: block;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: #1D1D1F;
    font-weight: 500;
  }
  
  .hostname-edit-icon {
    visibility: hidden;
    color: #007AFF;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
    margin-left: 8px;
    
    &:hover {
      color: #0050A0;
    }
  }
  
  &:hover {
    .hostname-edit-icon {
      visibility: visible;
    }
  }
}

.form-help-text {
  color: #888;
  font-size: 12px;
  display: flex;
  align-items: center;
  margin-top: 4px;
  
  .anticon {
    margin-right: 4px;
    font-size: 14px;
  }
}

.hostname-tooltip {
  padding: 0 !important;
  
  .hostname-detail-tooltip {
    display: block;
    padding: 0;
    margin: 0;
  
    .ant-tooltip-arrow {
      display: none;
    }
  }
  
  .ant-tooltip-content {
    border-radius: 16px !important;
    overflow: hidden;
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05) !important;
  }
  
  .ant-tooltip-inner {
    background-color: transparent !important;
    padding: 0 !important;
    max-width: 420px !important;
  }
}

.apple-style-modal {
  .ant-modal-content {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  }

  .ant-modal-header {
    padding: 20px 24px;
    background: @apple-white;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);

    .ant-modal-title {
      .apple-font();
      font-size: 1.2rem;
      font-weight: 500;
      color: @apple-black;
    }
  }

  .ant-modal-body {
    padding: 24px;
  }

  .ant-modal-footer {
    border-top: 1px solid rgba(0, 0, 0, 0.05);
    padding: 16px 24px;

    .ant-btn {
      height: 36px;
      padding: 0 18px;
      font-size: 0.95rem;
      font-weight: 500;
      border-radius: 18px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

      &.ant-btn-primary {
        background: @apple-blue;
        border: none;

        &:hover {
          background: @apple-blue-hover;
        }
      }

      &.ant-btn-dangerous {
        background: @apple-red;
        border: none;
        color: white;

        &:hover {
          background: darken(@apple-red, 5%);
        }
      }

      &:not(.ant-btn-primary):not(.ant-btn-dangerous) {
        background: @apple-gray-light;
        border: none;
        color: @apple-black;

        &:hover {
          background: darken(@apple-gray-light, 5%);
        }
      }
    }
  }
}

.apple-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background-color: @apple-white;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.modal-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: @apple-gray-light;
  margin-right: 16px;
}

.modal-title {
  .apple-font();
  font-size: 1.2rem;
  font-weight: 500;
  color: @apple-black;
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: @apple-gray-light;
  cursor: pointer;
  transition: background-color 0.3s;

  &:hover {
    background-color: darken(@apple-gray-light, 5%);
  }
}

.apple-modal-content {
  padding: 24px;
}

.host-info-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.info-label {
  .apple-font();
  font-size: 14px;
  font-weight: 500;
  color: @apple-black;
}

.info-value {
  .apple-font();
  font-size: 14px;
  color: @apple-black;
}

.current-hostname {
  color: @apple-gray;
}

.input-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-label {
  .apple-font();
  font-size: 14px;
  font-weight: 500;
  color: @apple-black;
}

.apple-input {
  width: 100%;
  height: 40px;
  padding: 10px;
  border: 1px solid @apple-gray;
  border-radius: 8px;
  font-size: 14px;
  color: @apple-black;
  transition: border-color 0.3s;

  &:focus {
    border-color: @apple-blue;
  }
}

.input-description {
  display: flex;
  align-items: center;
  gap: 8px;
}

.apple-modal-actions {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.apple-button.secondary {
  background-color: @apple-gray-light;
  border: 1px solid @apple-gray;
  color: @apple-black;

  &:hover {
    background-color: darken(@apple-gray-light, 5%);
    border-color: darken(@apple-gray, 10%);
  }
}

.apple-button.primary {
  background-color: @apple-blue;
  border: none;
  color: white;

  &:hover {
    background-color: darken(@apple-blue, 5%);
  }
}

.button-loader {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid white;
  border-radius: 50%;
  border-top-color: transparent;
  animation: spin 0.75s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.hostname-edit-modal {
  .apple-modal-header {
    display: flex;
    align-items: center;
    padding: 16px;
    background-color: #ffffff;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  }
  
  .modal-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background-color: rgba(0, 122, 255, 0.1);
    margin-right: 12px;
  }
  
  .modal-title {
    flex: 1;
    font-size: 16px;
    font-weight: 600;
    color: #1d1d1f;
  }
  
  .modal-close {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    cursor: pointer;
    transition: background-color 0.2s;
    color: #8e8e93;
    
    &:hover {
      background-color: rgba(0, 0, 0, 0.05);
    }
  }
  
  .apple-modal-content {
    padding: 20px;
    background-color: #ffffff;
  }
  
  .host-info-section {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  }
  
  .info-row {
    display: flex;
    align-items: center;
  }
  
  .info-label {
    width: 100px;
    font-size: 14px;
    font-weight: 500;
    color: #8e8e93;
  }
  
  .info-value {
    flex: 1;
    font-size: 14px;
    color: #1d1d1f;
  }
  
  .current-hostname {
    font-weight: 500;
  }
  
  .input-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 24px;
  }
  
  .input-label {
    font-size: 14px;
    font-weight: 500;
    color: #1d1d1f;
    margin-bottom: 4px;
  }
  
  .apple-input {
    width: 100%;
    height: 40px;
    padding: 0 12px;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 8px;
    font-size: 14px;
    color: #1d1d1f;
    transition: all 0.2s;
    
    &:focus {
      outline: none;
      border-color: #007aff;
      box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.2);
    }
    
    &::placeholder {
      color: #8e8e93;
    }
  }
  
  .input-description {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #8e8e93;
    margin-top: 4px;
  }
  
  .apple-modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 24px;
  }
  
  .apple-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 36px;
    padding: 0 16px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    border: none;
    
    &.secondary {
      background-color: rgba(0, 0, 0, 0.05);
      color: #1d1d1f;
      
      &:hover {
        background-color: rgba(0, 0, 0, 0.1);
      }
    }
    
    &.primary {
      background-color: #007aff;
      color: #ffffff;
      
      &:hover {
        background-color: #0069d9;
      }
      
      &:disabled {
        background-color: rgba(0, 122, 255, 0.5);
        cursor: not-allowed;
      }
    }
  }
  
  .button-loader {
    display: inline-block;
    width: 16px;
    height: 16px;
    border: 2px solid #ffffff;
    border-radius: 50%;
    border-top-color: transparent;
    animation: spin 0.75s linear infinite;
  }
  
  @keyframes spin {
    to { transform: rotate(360deg); }
  }
}

.apple-style-modal.fix-confirm-modal {
  .ant-modal-content {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  }

  .ant-modal-header {
    padding: 20px 24px;
    background: @apple-white;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);

    .ant-modal-title {
      .apple-font();
      font-size: 1.2rem;
      font-weight: 500;
      color: @apple-black;
    }
  }

  .ant-modal-body {
    padding: 24px;
  }

  .ant-modal-footer {
    border-top: 1px solid rgba(0, 0, 0, 0.05);
    padding: 16px 24px;

    .ant-btn {
      height: 36px;
      padding: 0 18px;
      font-size: 0.95rem;
      font-weight: 500;
      border-radius: 18px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

      &.ant-btn-primary {
        background: @apple-blue;
        border: none;

        &:hover {
          background: @apple-blue-hover;
        }
      }

      &.ant-btn-dangerous {
        background: @apple-red;
        border: none;
        color: white;

        &:hover {
          background: darken(@apple-red, 5%);
        }
      }

      &:not(.ant-btn-primary):not(.ant-btn-dangerous) {
        background: @apple-gray-light;
        border: none;
        color: @apple-black;

        &:hover {
          background: darken(@apple-gray-light, 5%);
        }
      }
    }
  }
}

.apple-modal-header.warning {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background-color: @apple-white;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.modal-icon.warning {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: @apple-orange;
  margin-right: 16px;
}

.modal-title {
  .apple-font();
  font-size: 1.2rem;
}

.fix-confirm-modal {
  .modal-icon.warning {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background-color: rgba(255, 149, 0, 0.1);
    margin-right: 12px;
    
    svg {
      stroke: #FF9500;
    }
  }
  
  .confirm-message-section {
    padding: 16px;
    margin-bottom: 16px;
    background-color: rgba(255, 149, 0, 0.05);
    border-radius: 8px;
    border-left: 4px solid #FF9500;
  }
  
  .confirm-message {
    font-size: 14px;
    line-height: 1.5;
    color: #1D1D1F;
    
    p {
      margin-bottom: 8px;
      
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
  
  .apple-button.danger {
    background-color: #FF3B30;
    color: #ffffff;
    
    &:hover {
      background-color: #E02D23;
    }
    
    &:disabled {
      background-color: rgba(255, 59, 48, 0.5);
      cursor: not-allowed;
    }
  }
}

.fix-confirm-modal.apple-card-modal {
  .ant-modal-content {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  }

  .ant-modal-header {
    padding: 20px 24px;
    background: @apple-white;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);

    .ant-modal-title {
      .apple-font();
      font-size: 1.2rem;
      font-weight: 500;
      color: @apple-black;
    }
  }

  .ant-modal-body {
    padding: 24px;
  }

  .ant-modal-footer {
    border-top: 1px solid rgba(0, 0, 0, 0.05);
    padding: 16px 24px;

    .ant-btn {
      height: 36px;
      padding: 0 18px;
      font-size: 0.95rem;
      font-weight: 500;
      border-radius: 18px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

      &.ant-btn-primary {
        background: @apple-blue;
        border: none;

        &:hover {
          background: @apple-blue-hover;
        }
      }

      &.ant-btn-dangerous {
        background: @apple-red;
        border: none;
        color: white;

        &:hover {
          background: darken(@apple-red, 5%);
        }
      }

      &:not(.ant-btn-primary):not(.ant-btn-dangerous) {
        background: @apple-gray-light;
        border: none;
        color: @apple-black;

        &:hover {
          background: darken(@apple-gray-light, 5%);
        }
      }
    }
  }
}

.apple-card-container {
  padding: 24px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin: 0px;
  font-family: "SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif;
}

.apple-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.apple-card-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(255, 149, 0, 0.1);
  margin-right: 16px;
}

.apple-card-icon-container.warning {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: @apple-orange;
}

.apple-card-info {
  flex: 1;
}

.apple-card-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: @apple-black;
}

.apple-card-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: background-color 0.3s;

  &:hover {
    background-color: rgba(0, a0, 0, 0.1);
  }
}

.apple-card-content {
  margin-bottom: 24px;
}

.apple-card-section {
  margin-bottom: 16px;
}

.apple-card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}
</style>
