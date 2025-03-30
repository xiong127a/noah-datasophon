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


 * @describe: step2-主机环境校验 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-08-15 14:07:04
 * @FilePath: \ddh-ui\src\components\steps\step2.vue
-->
<template>
  <div class="steps2 steps">
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

    <!-- 日志查看弹窗 -->
    <a-modal
      v-model="logVisible"
      :title="logModalTitle"
      width="80%"
      :footer="null"
      @cancel="closeLogModal"
      class="log-modal apple-modal"
      :bodyStyle="{ padding: '0' }"
    >
      <div class="log-container">
        <div class="log-header">
          <!-- 刷新控制区域 -->
          <div class="header-section">
            <div class="refresh-options">
              <a-button type="primary" @click="refreshLog" :loading="logLoading" class="refresh-btn">
                <a-icon type="reload" />手动刷新
              </a-button>
              <a-dropdown>
                <a-button :type="autoRefreshInterval > 0 ? 'primary' : 'default'" class="auto-refresh-btn">
                  <a-icon :type="autoRefreshInterval > 0 ? 'sync' : 'clock-circle'" :spin="autoRefreshInterval > 0" />
                  <span v-if="autoRefreshInterval === 0">开启自动刷新</span>
                  <span v-else>每 {{autoRefreshInterval}} 秒刷新中</span>
                  <a-icon type="down" style="margin-left: 4px" />
                </a-button>
                <a-menu slot="overlay" @click="handleAutoRefreshChange">
                  <a-menu-item key="0">
                    <a-icon type="stop" />关闭自动刷新
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="1">
                    <a-icon type="sync" />每秒刷新
                  </a-menu-item>
                  <a-menu-item key="3">
                    <a-icon type="sync" />每3秒刷新
                  </a-menu-item>
                  <a-menu-item key="5">
                    <a-icon type="sync" />每5秒刷新
                  </a-menu-item>
                  <a-menu-item key="10">
                    <a-icon type="sync" />每10秒刷新
                  </a-menu-item>
                </a-menu>
              </a-dropdown>
            </div>
          </div>
          
          <!-- 筛选控制区域 -->
          <div class="header-section filter-area">
            <!-- 日志类型选择 -->
            <div class="log-type-selector">
              <div class="filter-title">日志类型筛选：</div>
              <a-radio-group v-model="currentLogType" button-style="solid" @change="handleLogTypeChange">
                <a-radio-button value="all">全部日志</a-radio-button>
                <a-radio-button value="check">检查日志</a-radio-button>
                <a-radio-button value="fix">修复日志</a-radio-button>
              </a-radio-group>
            </div>
            
            <!-- 添加日志筛选组件 -->
            <log-filter 
              ref="logFilter"
              v-if="checkItem && checkItem.clusterId && showLogFilterOptions" 
              :clusterId="checkItem.clusterId" 
              :ip="checkItem.ip" 
              :itemId="checkItem.id"
              v-model="logContent"
              hide-reset-button
              @filter-change="handleFilterChange"
            ></log-filter>
          </div>
          
          <!-- 合并的筛选状态显示区域 -->
          <div class="combined-filter-status">
            <div class="filter-description">
              当前显示: 
              <span class="highlight">{{ currentLogType === 'all' ? '全部' : (currentLogType === 'check' ? '检查' : '修复') }}</span> 类型，
              <span v-if="checkItem && $refs.logFilter">
                <template v-if="$refs.logFilter.filterType === 'exact'">
                  <span class="highlight">{{ $refs.logFilter.selectedLevel }}</span> 级别
                </template>
                <template v-else-if="$refs.logFilter.filterType === 'min'">
                  <span class="highlight">{{ $refs.logFilter.selectedLevel }}</span> 及以上级别
                </template>
                <template v-else>
                  <span class="highlight">全部</span> 级别
                </template>
              </span>
              <span v-else>加载中...</span>
              的日志
            </div>
          </div>
        </div>
        <div class="log-content" :class="{'loading-container': logLoading}">
          <div v-if="logLoading" class="custom-loading">
            <a-icon type="loading" spin />
            <span>加载中...</span>
          </div>
          <pre v-html="logContent"></pre>
        </div>
      </div>
    </a-modal>

    <!-- 确认弹窗 -->
    <a-modal
      v-model="fixConfirmVisible"
      :title="fixConfirmTitle"
      :confirmLoading="fixConfirmLoading"
      @ok="handleFixConfirm"
      @cancel="handleFixCancel"
      okText="确认修复"
      cancelText="取消"
      okType="danger"
      class="fix-confirm-modal apple-modal"
    >
      <div class="fix-confirm-content">
        <div v-html="fixConfirmContent"></div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import LogFilter from '../log/LogFilter.vue';
import QueueStatusIndicator from '@/components/QueueStatusIndicator'

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps1Data: Object,
    depType:String,
  },
  components: {
    LogFilter,
    QueueStatusIndicator
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
      showLogFilterOptions: true, // 是否显示日志筛选选项
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
          width: 180,
          customRender: (text, row) => {
            const h = this.$createElement;
            
            // 优先检查SSH连接状态
            if (row.sshConnectStatus === 'error' || row.hasSSHError === true) {
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
                'SSH连接失败'
              ]);
            }
            
            // 修改判断顺序，优先检查状态
            // 1. 首先检查hostnameStatus状态（最高优先级）
            if (this.checkStatus(row.hostnameStatus, 'loading')) {
              // 创建加载中的主机名信息浮窗 - 苹果风格设计
              const loadingTooltipContent = h('div', { class: 'hostname-detail-loading' }, [
                // 添加顶部加载动画区域
                h('div', { class: 'hostname-detail-loading-header' }),
                
                // 中间内容区域加载动画
                h('div', { class: 'hostname-detail-loading-content' }, [
                  h('div', { class: 'hostname-detail-loading-line short' }),
                  h('div', { class: 'hostname-detail-loading-line medium' }),
                  h('div', { class: 'hostname-detail-loading-line' }),
                  h('div', { class: 'hostname-detail-loading-line short' }),
                  h('div', { class: 'hostname-detail-loading-line medium' })
                ]),
                
                // 底部加载文字
                h('div', { class: 'hostname-detail-loading-text' }, [
                  '正在获取主机信息...'
                ])
              ]);
              
              // 苹果风格的加载状态显示，与操作系统一致
              return h('a-tooltip', {
                props: {
                  placement: 'right',
                  arrowPointAtCenter: true,
                  overlayClassName: 'hostname-tooltip',
                  getPopupContainer: () => document.body
                }
              }, [
                // 加载中浮窗内容
                h('span', { 
                  slot: 'title',
                  class: 'hostname-detail-tooltip-container'
                }, [loadingTooltipContent]),
                
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
                      fontSize: '13px', 
                      color: '#8E8E93',
                      fontWeight: '500',
                      position: 'relative',
                      zIndex: 2,
                      whiteSpace: 'nowrap'
                    }
                  }, ['正在获取主机名...'])
                ])
              ]);
            }
            
            // 2. 其次检查是否有错误状态
            if (this.checkStatus(row.hostnameStatus, 'error')) {
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
                '主机名获取失败'
              ]);
            }
            
            // 3. 检查成功状态
            if (this.checkStatus(row.hostnameStatus, 'success')) {
              // 主机名收集成功但hostname为null的情况
              if (row.hostname === null) {
                return h('div', {
                  style: {
                    display: 'flex',
                    alignItems: 'center',
                    color: '#8E8E93'
                  }
                }, [
                  h('span', {}, ['主机名为空'])
                ]);
              }
            }
            
            // 4. 最后检查主机名是否为null（最低优先级）
            if (row.hostname === null) {
              return h('div', {
                style: {
                  display: 'flex',
                  alignItems: 'center',
                  color: '#8E8E93'
                }
              }, [
                h('span', {}, ['正在获取主机名...'])
              ]);
            }
            
            // 4. 最后是正常显示主机名
            // 正常显示主机名，添加悬浮卡片
            const tooltipContent = h('div', { class: 'hostname-detail-tooltip' }, [
              // 主机名信息部分
              h('div', { class: 'hostname-detail-section' }, [
                h('div', { class: 'hostname-detail-title' }, [
                  h('span', { style: { display: 'flex', alignItems: 'center' } }, [
                    h('svg', {
                      attrs: {
                        viewBox: '0 0 24 24',
                        width: '16',
                        height: '16',
                        fill: 'none',
                        stroke: '#007AFF',
                        'stroke-width': '2',
                        'stroke-linecap': 'round',
                        'stroke-linejoin': 'round'
                      },
                      style: { marginRight: '8px' }
                    }, [
                      h('path', { attrs: { d: 'M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z' } }),
                      h('polyline', { attrs: { points: '9 22 9 12 15 12 15 22' } })
                    ]),
                    '主机基本信息'
                  ])
                ]),
                h('div', { class: 'hostname-detail-content' }, [
                  h('div', { class: 'hostname-detail-item' }, [
                    h('span', { class: 'hostname-detail-label' }, ['主机名:']),
                    h('span', { class: 'hostname-detail-value' }, [row.hostname || '-'])
                  ]),
                  h('div', { class: 'hostname-detail-item' }, [
                    h('span', { class: 'hostname-detail-label' }, ['完整域名:']),
                    h('span', { class: 'hostname-detail-value' }, [row.fqdn || '-'])
                  ]),
                  h('div', { class: 'hostname-detail-item' }, [
                    h('span', { class: 'hostname-detail-label' }, ['IP地址:']),
                    h('span', { class: 'hostname-detail-value' }, [row.ip || '-'])
                  ])
                ])
              ]),
              
              // DNS服务器信息部分
              h('div', { class: 'hostname-detail-section' }, [
                h('div', { class: 'hostname-detail-title' }, [
                  h('span', { style: { display: 'flex', alignItems: 'center' } }, [
                    h('svg', {
                      attrs: {
                        viewBox: '0 0 24 24',
                        width: '16',
                        height: '16',
                        fill: 'none',
                        stroke: '#5AC8FA',
                        'stroke-width': '2',
                        'stroke-linecap': 'round',
                        'stroke-linejoin': 'round'
                      },
                      style: { marginRight: '8px' }
                    }, [
                      h('path', { attrs: { d: 'M22 12h-4l-3 9L9 3l-3 9H2' } })
                    ]),
                    '网络配置信息'
                  ])
                ]),
                h('div', { class: 'hostname-detail-content' }, [
                  h('div', { class: 'hostname-detail-item' }, [
                    h('span', { class: 'hostname-detail-label' }, ['DNS服务器:']),
                    h('span', { class: 'hostname-detail-value dns-servers' }, [
                      row.osInfo && row.osInfo.dnsServers ? row.osInfo.dnsServers : '未配置DNS'
                    ])
                  ])
                ])
              ]),
              
              // hosts文件信息部分
              h('div', { class: 'hostname-detail-section' }, [
                h('div', { class: 'hostname-detail-title' }, [
                  h('span', { style: { display: 'flex', alignItems: 'center' } }, [
                    h('svg', {
                      attrs: {
                        viewBox: '0 0 24 24',
                        width: '16',
                        height: '16',
                        fill: 'none',
                        stroke: '#FF9500',
                        'stroke-width': '2',
                        'stroke-linecap': 'round',
                        'stroke-linejoin': 'round'
                      },
                      style: { marginRight: '8px' }
                    }, [
                      h('path', { attrs: { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' } }),
                      h('polyline', { attrs: { points: '14 2 14 8 20 8' } }),
                      h('line', { attrs: { x1: '16', y1: '13', x2: '8', y2: '13' } }),
                      h('line', { attrs: { x1: '16', y1: '17', x2: '8', y2: '17' } }),
                      h('polyline', { attrs: { points: '10 9 9 9 8 9' } })
                    ]),
                    'Hosts文件内容'
                  ])
                ]),
                h('div', { class: 'hostname-detail-content' }, [
                  row.hostsFile ? 
                    h('pre', { class: 'hostname-detail-hosts-file' }, [row.hostsFile]) : 
                    h('div', { class: 'hostname-detail-empty' }, ['暂无hosts文件内容'])
                ])
              ])
            ]);
            
            return h('a-tooltip', {
              props: {
                placement: 'right',
                arrowPointAtCenter: true,
                overlayClassName: 'hostname-tooltip',
                getPopupContainer: () => document.body
              }
            }, [
              // 悬浮显示的内容
              h('span', { slot: 'title' }, [tooltipContent]),
              
              // 显示的主机名（可点击）
              h('span', {
                class: 'hostname-display',
                style: {
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center'
                }
              }, [
                row.hostname,
                h('svg', {
                  attrs: {
                    viewBox: '0 0 24 24',
                    width: '14',
                    height: '14',
                    fill: 'none',
                    stroke: 'currentColor',
                    'stroke-width': '2',
                    'stroke-linecap': 'round',
                    'stroke-linejoin': 'round'
                  },
                  style: { 
                    marginLeft: '6px',
                    color: '#8E8E93',
                    opacity: 0.8
                  }
                }, [
                  h('circle', { attrs: { cx: '12', cy: '12', r: '10' } }),
                  h('line', { attrs: { x1: '12', y1: '16', x2: '12', y2: '12' } }),
                  h('line', { attrs: { x1: '12', y1: '8', x2: '12.01', y2: '8' } })
                ])
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
            
            // 检查osInfoStatus状态，显示加载动画
            // 当osInfoStatus为loading或osInfo为null时都显示加载动画
            if (this.checkStatus(row.osInfoStatus, 'loading') || 
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

              // 创建加载中的操作系统信息浮窗
              const loadingTooltipContent = h('div', { class: 'os-detail-loading' }, [
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
                }, ['正在获取操作系统信息...'])
              ]);
              
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
                }, [loadingTooltipContent]),
                
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
            const hasOsInfo = row.osInfo && row.osInfo.valid;
            const osType = hasOsInfo ? row.osInfo.distribution : (text || row.osType || '-');
            const osVersion = hasOsInfo ? row.osInfo.versionId : (row.osVersion || '');
            
            // 获取操作系统对应的图标路径
            const getOsIconPath = (osType) => {
              const osLower = (osType || '').toLowerCase();
              if (osLower.includes('centos')) {
                return require('@/assets/os-logos/centos.svg');
              } else if (osLower.includes('ubuntu')) {
                return require('@/assets/os-logos/ubuntu.svg');
              } else if (osLower.includes('debian')) {
                return require('@/assets/os-logos/debian.svg');
              } else if (osLower.includes('redhat') || osLower.includes('red hat')) {
                return require('@/assets/os-logos/redhat.svg');
              } else if (osLower.includes('windows')) {
                return require('@/assets/os-logos/windows.svg');
              } else if (osLower.includes('kylin') || osLower.includes('麒麟')) {
                return require('@/assets/os-logos/kylin.svg');
              } else {
                return require('@/assets/os-logos/linux-tux.svg');
              }
            };
            
            // 获取操作系统对应的颜色
            const getOsColor = (osType) => {
              const osLower = (osType || '').toLowerCase();
              if (osLower.includes('centos')) {
                return '#932279';
              } else if (osLower.includes('ubuntu')) {
                return '#E95420';
              } else if (osLower.includes('debian')) {
                return '#D70A53';
              } else if (osLower.includes('redhat') || osLower.includes('red hat')) {
                return '#EE0000';
              } else if (osLower.includes('windows')) {
                return '#0078D6';
              } else if (osLower.includes('kylin') || osLower.includes('麒麟')) {
                return '#0066B3';
              } else {
                return '#87d068';
              }
            };
            
            const iconPath = getOsIconPath(osType);
            const color = getOsColor(osType);
            
            // 创建详细的操作系统信息弹出框（添加硬件信息部分）
            const osDetailContent = hasOsInfo ? h('div', { class: 'os-detail-popup' }, [
              // 标题区域
              h('div', { class: 'os-detail-header' }, [
                h('div', { class: 'os-detail-icon-container' }, [
                  h('img', {
                    attrs: {
                      src: iconPath,
                      alt: osType,
                      width: '64',
                      height: '64'
                    },
                    class: 'os-img-no-filter',
                    style: {
                      filter: 'none !important',
                      borderRadius: '12px'
                    }
                  })
                ]),
                h('div', { class: 'os-detail-title-container' }, [
                  h('h3', { class: 'os-detail-title' }, [row.osInfo.fullName || `${row.osInfo.distribution} ${row.osInfo.versionId}`]),
                  h('div', { class: 'os-detail-subtitle' }, [row.osInfo.kernelVersion ? `内核版本 ${row.osInfo.kernelVersion}` : '']),
                  h('div', { class: 'os-detail-subtitle' }, [row.osInfo.architecture ? `${row.osInfo.architecture} 架构` : ''])
                ])
              ]),
              
              // 内容区域包装元素
              h('div', { class: 'os-detail-content' }, [
                // 硬件信息卡片
                h('div', { class: 'os-detail-card' }, [
                  h('div', { class: 'os-detail-card-header' }, [
                    h('i', { class: 'anticon anticon-desktop', style: { marginRight: '8px', color: '#007AFF' }}),
                    h('span', {}, ['硬件信息'])
                  ]),
                  
                  // CPU信息
                  h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { 
                        class: 'os-detail-info-icon cpu',
                        style: {
                          backgroundColor: this.checkStatus(row.cpuStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' : 
                                          this.checkStatus(row.cpuStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' : 
                                          'rgba(0, 122, 255, 0.1)',
                          color: this.checkStatus(row.cpuStatus, 'success') ? '#34C759' : 
                                this.checkStatus(row.cpuStatus, 'error') ? '#FF3B30' : 
                                '#007AFF'
                        }
                      }, [
                        h('i', { class: 'anticon anticon-api' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, [
                        h('span', {}, ['处理器']),
                        // 添加状态图标
                        this.checkStatus(row.cpuStatus, 'success') ? 
                          h('a-icon', { 
                            props: { type: 'check-circle' }, 
                            style: { marginLeft: '6px', color: '#34C759', fontSize: '12px' } 
                          }) :
                          this.checkStatus(row.cpuStatus, 'error') ?
                            h('a-icon', { 
                              props: { type: 'close-circle' }, 
                              style: { marginLeft: '6px', color: '#FF3B30', fontSize: '12px' } 
                            }) :
                            this.checkStatus(row.cpuStatus, 'loading') ?
                              h('a-icon', { 
                                props: { type: 'loading' }, 
                                style: { marginLeft: '6px', color: '#007AFF', fontSize: '12px' } 
                              }) : null
                      ]),
                      row.cpuStatus === 'loading' ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', {}, ['正在收集CPU信息...'])
                        ]) :
                        row.cpuStatus === 'error' ?
                          h('div', { class: 'os-detail-info-value error' }, [
                            h('a-icon', { props: { type: 'warning' }, style: { marginRight: '6px' } }),
                            '获取CPU信息失败'
                          ]) :
                          row.cpuStatus === 'pending' ?
                            h('div', { class: 'os-detail-info-value waiting' }, [
                              h('a-icon', { props: { type: 'clock-circle' }, style: { marginRight: '6px', color: '#FAAD14' } }),
                              '等待收集CPU信息'
                            ]) :
                          h('div', { class: 'os-detail-info-value' }, [
                            // 改为显示更多CPU详情，包括型号、数量、核心数和线程数
                            row.osInfo && row.osInfo.cpuModel ? 
                              h('div', {}, [
                                h('div', { style: { fontWeight: '500' } }, [row.osInfo.cpuModel]),
                                h('div', { style: { fontSize: '12px', color: '#666', marginTop: '4px' } }, [
                                  h('span', {}, [
                                    `${row.osInfo.cpuCount || 1} 个处理器 × ${row.osInfo.cpuCores || 1} 核心/处理器 × ${row.osInfo.cpuThreadsPerCore || 1} 线程/核心 = ${row.osInfo.cpuLogicalCores || 1} 逻辑核心`
                                  ]),
                                  row.osInfo.cpuFrequency && row.osInfo.cpuFrequency > 0 ? 
                                    h('span', { style: { marginLeft: '4px' } }, [`(${row.osInfo.cpuFrequency.toFixed(1)} GHz)`]) : null
                                ])
                              ]) : '未知'
                      ])
                    ])
                  ]),
                  
                  // 内存信息
                  h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { 
                        class: 'os-detail-info-icon memory',
                        style: {
                          backgroundColor: this.checkStatus(row.memoryStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' : 
                                          this.checkStatus(row.memoryStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' : 
                                          'rgba(0, 122, 255, 0.1)',
                          color: this.checkStatus(row.memoryStatus, 'success') ? '#34C759' : 
                                this.checkStatus(row.memoryStatus, 'error') ? '#FF3B30' : 
                                '#007AFF'
                        }
                      }, [
                        h('i', { class: 'anticon anticon-database' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, [
                        h('span', {}, ['内存']),
                        // 添加状态图标
                        this.checkStatus(row.memoryStatus, 'success') ? 
                          h('a-icon', { 
                            props: { type: 'check-circle' }, 
                            style: { marginLeft: '6px', color: '#34C759', fontSize: '12px' } 
                          }) :
                          this.checkStatus(row.memoryStatus, 'error') ?
                            h('a-icon', { 
                              props: { type: 'close-circle' }, 
                              style: { marginLeft: '6px', color: '#FF3B30', fontSize: '12px' } 
                            }) :
                            this.checkStatus(row.memoryStatus, 'loading') ?
                              h('a-icon', { 
                                props: { type: 'loading' }, 
                                style: { marginLeft: '6px', color: '#007AFF', fontSize: '12px' } 
                              }) : null
                      ]),
                      row.memoryStatus === 'loading' ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', {}, ['正在收集内存信息...'])
                        ]) :
                        this.checkStatus(row.memoryStatus, 'error') ?
                          h('div', { class: 'os-detail-info-value error' }, [
                            h('a-icon', { props: { type: 'warning' }, style: { marginRight: '6px' } }),
                            '获取内存信息失败'
                          ]) :
                          this.checkStatus(row.memoryStatus, 'pending') ?
                            h('div', { class: 'os-detail-info-value waiting' }, [
                              h('a-icon', { props: { type: 'clock-circle' }, style: { marginRight: '6px', color: '#FAAD14' } }),
                              '等待收集内存信息'
                            ]) :
                          h('div', { class: 'os-detail-info-value' }, [
                            // 改为显示更详细的内存信息
                            row.osInfo && row.osInfo.totalMemory ? 
                              h('div', {}, [
                                h('div', { style: { fontWeight: '500' } }, [
                                  `总内存: ${row.osInfo.totalMemory} GB`
                                ]),
                                h('div', { style: { fontSize: '12px', color: '#666', marginTop: '4px' } }, [
                                  `已用: ${(row.osInfo.totalMemory - row.osInfo.availableMemory).toFixed(1)} GB，可用: ${row.osInfo.availableMemory} GB`
                                ]),
                                h('div', { style: { marginTop: '6px' } }, [
                                  h('div', { style: { display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '2px' } }, [
                                    h('span', {}, [`使用率: ${(100 * (1 - row.osInfo.availableMemory / row.osInfo.totalMemory)).toFixed(1)}%`]),
                                    h('span', {}, [`${(row.osInfo.totalMemory - row.osInfo.availableMemory).toFixed(1)}/${row.osInfo.totalMemory} GB`])
                                  ]),
                                  h('a-progress', {
                                    props: {
                                      percent: Number((100 * (1 - row.osInfo.availableMemory / row.osInfo.totalMemory)).toFixed(1)),
                                      showInfo: false,
                                      strokeColor: {
                                        '0%': '#108ee9',
                                        '100%': '#87d068',
                                      },
                                      strokeWidth: 6
                                    }
                                  })
                                ])
                              ]) : '未知'
                      ])
                    ])
                  ]),
                  
                  // 磁盘信息
                  h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { 
                        class: 'os-detail-info-icon storage',
                        style: {
                          backgroundColor: this.checkStatus(row.diskStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' : 
                                          this.checkStatus(row.diskStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' : 
                                          'rgba(0, 122, 255, 0.1)',
                          color: this.checkStatus(row.diskStatus, 'success') ? '#34C759' : 
                                this.checkStatus(row.diskStatus, 'error') ? '#FF3B30' : 
                                '#007AFF'
                        }
                      }, [
                        h('i', { class: 'anticon anticon-hdd' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, [
                        h('span', {}, ['磁盘']),
                        // 添加状态图标
                        this.checkStatus(row.diskStatus, 'success') ? 
                          h('a-icon', { 
                            props: { type: 'check-circle' }, 
                            style: { marginLeft: '6px', color: '#34C759', fontSize: '12px' } 
                          }) :
                          this.checkStatus(row.diskStatus, 'error') ?
                            h('a-icon', { 
                              props: { type: 'close-circle' }, 
                              style: { marginLeft: '6px', color: '#FF3B30', fontSize: '12px' } 
                            }) :
                            this.checkStatus(row.diskStatus, 'loading') ?
                              h('a-icon', { 
                                props: { type: 'loading' }, 
                                style: { marginLeft: '6px', color: '#007AFF', fontSize: '12px' } 
                              }) : null
                      ]),
                      row.diskStatus === 'loading' ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', {}, ['正在收集磁盘信息...'])
                        ]) :
                        row.diskStatus === 'error' ?
                          h('div', { class: 'os-detail-info-value error' }, [
                            h('a-icon', { props: { type: 'warning' }, style: { marginRight: '6px' } }),
                            '获取磁盘信息失败'
                          ]) :
                          row.diskStatus === 'pending' ?
                            h('div', { class: 'os-detail-info-value waiting' }, [
                              h('a-icon', { props: { type: 'clock-circle' }, style: { marginRight: '6px', color: '#FAAD14' } }),
                              '等待收集磁盘信息'
                            ]) :
                          h('div', { class: 'os-detail-info-value' }, [
                            // 改为显示更详细的磁盘信息
                            row.osInfo && row.osInfo.totalDisk ? 
                              h('div', {}, [
                                h('div', { style: { fontWeight: '500' } }, [
                                  `总空间: ${row.osInfo.totalDisk} GB`
                                ]),
                                h('div', { style: { fontSize: '12px', color: '#666', marginTop: '4px' } }, [
                                  `已用: ${(row.osInfo.totalDisk - row.osInfo.availableDisk).toFixed(1)} GB，可用: ${row.osInfo.availableDisk} GB`
                                ]),
                                h('div', { style: { marginTop: '6px' } }, [
                                  h('div', { style: { display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '2px' } }, [
                                    h('span', {}, [
                                      `使用率: ${(100 * (1 - row.osInfo.availableDisk / row.osInfo.totalDisk)).toFixed(1)}%`
                                    ]),
                                    h('span', {}, [
                                      `${(row.osInfo.totalDisk - row.osInfo.availableDisk).toFixed(1)}/${row.osInfo.totalDisk} GB`
                                    ])
                                  ]),
                                  h('a-progress', {
                                    props: {
                                      percent: Number((100 * (1 - row.osInfo.availableDisk / row.osInfo.totalDisk)).toFixed(1)),
                                      showInfo: false,
                                      strokeColor: (100 * (1 - row.osInfo.availableDisk / row.osInfo.totalDisk) > 90) ? '#ff4d4f' : 
                                                  (100 * (1 - row.osInfo.availableDisk / row.osInfo.totalDisk) > 70) ? '#faad14' : '#52c41a',
                                      strokeWidth: 6
                                    }
                                  })
                                ])
                              ]) : '未知'
                      ])
                    ])
                  ]),
                  
                  // 交换空间信息
                  h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { 
                        class: 'os-detail-info-icon swap',
                        style: {
                          backgroundColor: this.checkStatus(row.swapStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' : 
                                          this.checkStatus(row.swapStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' : 
                                          'rgba(0, 122, 255, 0.1)',
                          color: this.checkStatus(row.swapStatus, 'success') ? '#34C759' : 
                                this.checkStatus(row.swapStatus, 'error') ? '#FF3B30' : 
                                '#007AFF'
                        }
                      }, [
                        h('i', { class: 'anticon anticon-swap' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, [
                        h('span', {}, ['交换空间']),
                        // 添加状态图标
                        this.checkStatus(row.swapStatus, 'success') ? 
                          h('a-icon', { 
                            props: { type: 'check-circle' }, 
                            style: { marginLeft: '6px', color: '#34C759', fontSize: '12px' } 
                          }) :
                          this.checkStatus(row.swapStatus, 'error') ?
                            h('a-icon', { 
                              props: { type: 'close-circle' }, 
                              style: { marginLeft: '6px', color: '#FF3B30', fontSize: '12px' } 
                            }) :
                            row.swapStatus === 'loading' ?
                              h('a-icon', { 
                                props: { type: 'loading' }, 
                                style: { marginLeft: '6px', color: '#007AFF', fontSize: '12px' } 
                              }) : 
                            row.swapStatus === 'pending' ?
                              h('a-icon', { 
                                props: { type: 'clock-circle' }, 
                                style: { marginLeft: '6px', color: '#FAAD14', fontSize: '12px' } 
                              }) : null
                      ]),
                      row.swapStatus === 'loading' ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', {}, ['正在收集交换空间信息...'])
                        ]) :
                        row.swapStatus === 'error' ?
                          h('div', { class: 'os-detail-info-value error' }, [
                            h('a-icon', { props: { type: 'warning' }, style: { marginRight: '6px' } }),
                            '获取交换空间信息失败'
                          ]) :
                          row.swapStatus === 'pending' ?
                            h('div', { class: 'os-detail-info-value waiting' }, [
                              h('a-icon', { props: { type: 'clock-circle' }, style: { marginRight: '6px', color: '#FAAD14' } }),
                              '等待收集交换空间信息'
                            ]) :
                          h('div', { class: 'os-detail-info-value' }, [
                            row.osInfo && row.osInfo.totalSwap !== undefined ? 
                              row.osInfo.totalSwap === 0 ? 
                                '未开启交换空间' : 
                                `交换空间: ${row.osInfo.totalSwap}GB` : 
                              '未知'
                          ])
                    ])
                  ]),
                  
                  // GPU信息
                  h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { 
                        class: 'os-detail-info-icon gpu',
                        style: {
                          backgroundColor: this.checkStatus(row.gpuStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' : 
                                          this.checkStatus(row.gpuStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' : 
                                          'rgba(0, 122, 255, 0.1)',
                          color: this.checkStatus(row.gpuStatus, 'success') ? '#34C759' : 
                                this.checkStatus(row.gpuStatus, 'error') ? '#FF3B30' : 
                                '#007AFF'
                        }
                      }, [
                        h('i', { class: 'anticon anticon-radar-chart' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, [
                        h('span', {}, ['GPU']),
                        // 添加状态图标
                        this.checkStatus(row.gpuStatus, 'success') ? 
                          h('a-icon', { 
                            props: { type: 'check-circle' }, 
                            style: { marginLeft: '6px', color: '#34C759', fontSize: '12px' } 
                          }) :
                          this.checkStatus(row.gpuStatus, 'error') ?
                            h('a-icon', { 
                              props: { type: 'close-circle' }, 
                              style: { marginLeft: '6px', color: '#FF3B30', fontSize: '12px' } 
                            }) :
                            this.checkStatus(row.gpuStatus, 'loading') ?
                              h('a-icon', { 
                                props: { type: 'loading' }, 
                                style: { marginLeft: '6px', color: '#007AFF', fontSize: '12px' } 
                              }) : null
                      ]),
                      row.gpuStatus === 'loading' ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', {}, ['正在收集GPU信息...'])
                        ]) :
                        row.gpuStatus === 'error' ?
                          h('div', { class: 'os-detail-info-value error' }, [
                            h('a-icon', { props: { type: 'warning' }, style: { marginRight: '6px' } }),
                            '获取GPU信息失败'
                          ]) :
                          row.gpuStatus === 'pending' ?
                            h('div', { class: 'os-detail-info-value waiting' }, [
                              h('a-icon', { props: { type: 'clock-circle' }, style: { marginRight: '6px', color: '#FAAD14' } }),
                              '等待收集GPU信息'
                            ]) :
                          h('div', { class: 'os-detail-info-value' }, [
                          // 改为显示更详细的GPU信息
                          row.osInfo ? 
                            h('div', {}, [
                              (row.osInfo.gpuInfo && !row.osInfo.gpuInfo.startsWith('ERROR:') && row.osInfo.gpuInfo !== '未检测到GPU设备') ? 
                                h('div', {}, [
                                  h('div', { style: { fontWeight: '500' } }, [
                                    row.osInfo.gpuInfo
                                  ]),
                                  h('div', { style: { fontSize: '12px', color: '#666', marginTop: '4px' } }, [
                                    row.osInfo.gpuMemory && row.osInfo.gpuMemory > 0 ? 
                                      `显存: ${row.osInfo.gpuMemory.toFixed(1)} GB` : 
                                      '显存信息未获取到'
                                  ])
                                ]) : 
                                h('div', { style: { color: '#666' } }, ['未检测到GPU设备或无法获取GPU信息'])
                            ]) : '未知'
                          ])
                    ])
                  ])
                ])
              ])
            ]) : null;
            
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
                // 悬浮显示的详细内容
                h('span', { 
                  slot: 'title',
                  class: 'os-detail-tooltip'
                }, [osDetailContent]),
                
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
                        alt: osType
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
                    }, [osType]),
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
      logContent: '',
      logLoading: false,
      autoRefreshInterval: 0,
      currentLogIp: null,
      currentLogItemId: null,
      currentLogItemName: null,
      refreshTimer: null,
      currentLogType: 'all',
      forceUseTypedApi: false,
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
      const host = record.host
      return h('div', { class: 'expand-content' }, [
        h('div', { class: 'expand-content-header' }, [
          h('span', { class: 'expand-content-title' }, [
            h('a-icon', { props: { type: 'desktop' }, style: { marginRight: '8px' } }),
            `主机 ${host}`
          ]),
          h('a-tag', {
            props: {
              color: this.hostOsInfo[host] && this.hostOsInfo[host].osVersion === 'CentOS 7' ? '#F15A29' : '#47B27B'
            }
          }, [
            this.hostOsInfo[host] ? this.hostOsInfo[host].osVersion : '获取中...'
          ])
        ]),
        h('div', { class: 'expand-content-body' }, [
          h('a-row', { props: { gutter: 16 } }, [
            h('a-col', { props: { span: 12 } }, [
              h('a-card', { class: 'expand-card', props: { bordered: false } }, [
                h('template', { slot: 'title' }, [
                  h('div', { style: { display: 'flex', alignItems: 'center', fontWeight: 500 } }, [
                    h('a-tooltip', {
                      props: {
                        placement: 'right',
                        overlayClassName: 'hostname-detail-tooltip',
                        mouseEnterDelay: 0.3,
                        destroyTooltipOnHide: true
                      }
                    }, [
                      h('div', {
                        style: { 
                          display: 'flex', 
                          alignItems: 'center', 
                          cursor: 'pointer' 
                        }
                      }, [
                        h('a-icon', { 
                          props: { type: 'info-circle' }, 
                          style: { marginRight: '8px', color: '#007AFF' } 
                        }),
                        '主机名'
                      ]),
                      h('div', { slot: 'title' }, [
                        // 主机名详情卡片
                        h('div', {}, [
                          h('div', { class: 'hostname-detail-title' }, [
                            h('div', { class: 'section-icon hostname-icon' }, [
                              h('a-icon', { props: { type: 'desktop' } })
                            ]),
                            '主机信息'
                          ]),
                          
                          h('div', { class: 'hostname-detail-content' }, [
                            h('div', { class: 'hostname-detail-item' }, [
                              h('div', { class: 'hostname-detail-label' }, ['主机名']),
                              h('div', { class: 'hostname-detail-value' }, [host])
                            ])
                          ]),
                          
                          h('div', { class: 'hostname-detail-title' }, [
                            h('div', { class: 'section-icon network-icon' }, [
                              h('a-icon', { props: { type: 'global' } })
                            ]),
                            '网络配置'
                          ]),
                          
                          h('div', { class: 'hostname-detail-content' }, [
                            h('div', { class: 'hostname-detail-item' }, [
                              h('div', { class: 'hostname-detail-label' }, ['DNS 服务器']),
                              h('div', { class: 'hostname-detail-value dns-servers' }, [
                                this.renderDnsServers(this.hostOsInfo[host] ? this.hostOsInfo[host].dnsServers : [])
                              ])
                            ])
                          ]),
                          
                          h('div', { class: 'hostname-detail-title' }, [
                            h('div', { class: 'section-icon hosts-icon' }, [
                              h('a-icon', { props: { type: 'file-text' } })
                            ]),
                            'Hosts 文件'
                          ]),
                          
                          h('div', { class: 'hostname-detail-content' }, [
                            h('div', { class: 'hostname-detail-item' }, [
                              h('div', { class: 'hostname-detail-label' }, ['Hosts 文件内容']),
                              h('div', { class: 'hostname-detail-hosts-file' }, [
                                this.renderHostsFile(this.hostOsInfo[host] ? this.hostOsInfo[host].hostsFile : '')
                              ])
                            ])
                          ])
                        ])
                      ])
                    ])
                  ])
                ]),
                h('a-statistic', {
                  props: {
                    title: '主机名',
                    value: host,
                    valueStyle: { fontWeight: 'bold', color: '#1D1D1F' }
                  }
                })
              ])
            ])
            // ... existing code ...
          ])
        ])
      ])
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
      // 保存当前选择的检查项信息
      this.currentLogIp = ip;
      this.currentLogItemId = itemId;
      this.currentLogItemName = itemName;
      
      // 设置当前检查项信息，用于日志筛选组件
      this.checkItem = {
        clusterId: this.clusterId,
        ip: ip,
        id: itemId,
        itemName: itemName
      };
      
      // 打开日志弹窗并加载日志
      this.logModalTitle = `日志 - 主机: ${ip}, 检查项: ${itemName}`;
      this.logVisible = true;
      this.logContent = '';
      
      // 设置初始日志类型为全部日志
      this.currentLogType = 'all';
      
      // 初始停止之前可能存在的自动刷新定时器
      this.stopAutoRefresh();

      // 等待DOM更新完成后设置初始筛选条件
      this.$nextTick(() => {
        if (this.$refs.logFilter) {
          // 设置默认筛选条件
          this.$refs.logFilter.filterType = 'min';
          this.$refs.logFilter.selectedLevel = 'INFO';
          // 手动触发筛选
          this.$refs.logFilter.applyFilter();
        }
        // 获取日志数据
        this.fetchItemLog();
      });
    },
    
    // 获取检查项日志
    async fetchItemLog() {
      if (!this.currentLogIp || !this.currentLogItemId) {
        return;
      }
      
      this.logLoading = true;
      
      try {
        // 统一使用一个API进行所有筛选
        const apiUrl = '/ddh/host/check/getLog';
        
        // 获取级别筛选参数
        let logLevel = 'INFO'; // 默认显示INFO级别
        let filterMode = 'min'; // 默认显示INFO及以上级别
        
        if (this.$refs.logFilter) {
          filterMode = this.$refs.logFilter.filterType;
          logLevel = this.$refs.logFilter.selectedLevel;
        }
          
        // 准备请求参数
        const params = { 
          clusterId: this.clusterId,
          ip: this.currentLogIp,
          itemId: this.currentLogItemId,
          logType: this.currentLogType,
          logLevel: logLevel,
          filterMode: filterMode
        };
        
        console.log('API请求:', apiUrl, params);
        const res = await this.$axiosPost(apiUrl, params);
        
        if (res.code === 200) {
          this.logContent = res.data || '暂无日志数据';
        } else {
          this.logContent = `获取日志失败: ${res.msg || '未知错误'}`;
          if (this.autoRefreshInterval > 0) {
            this.stopAutoRefresh(); // 如果获取失败，停止自动刷新
            this.$message.error('日志获取失败，已停止自动刷新');
          }
        }
      } catch (error) {
        console.error('获取日志失败:', error);
        this.logContent = '获取日志失败，请稍后重试';
        if (this.autoRefreshInterval > 0) {
          this.stopAutoRefresh(); // 如果获取失败，停止自动刷新
          this.$message.error('日志获取失败，已停止自动刷新');
        }
      } finally {
        this.logLoading = false;
      }
    },
    
    // 手动刷新日志
    refreshLog() {
      this.fetchItemLog();
    },
    
    // 处理自动刷新间隔变化
    handleAutoRefreshChange(e) {
      const value = parseInt(e.key);
      
      // 停止之前的自动刷新
      this.stopAutoRefresh();
      
      // 设置新的自动刷新间隔
      this.autoRefreshInterval = value;
      
      // 如果选择了自动刷新，启动定时器
      if (value > 0) {
        this.$message.success(`已开启自动刷新(${value}秒)`);
        this.startAutoRefresh();
      } else {
        this.$message.info('已关闭自动刷新');
      }
    },
    
    // 启动自动刷新
    startAutoRefresh() {
      if (this.autoRefreshInterval > 0) {
        this.refreshTimer = setInterval(() => {
          this.fetchItemLog();
        }, this.autoRefreshInterval * 1000);
      }
    },
    
    // 停止自动刷新
    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    },
    
    // 重置日志筛选，显示INFO级以上的全部类型日志
    resetLogFilter() {
      this.currentLogType = 'all';
      if (this.$refs.logFilter) {
        this.$refs.logFilter.filterType = 'min';
        this.$refs.logFilter.selectedLevel = 'INFO';
        this.$refs.logFilter.applyFilter();
      }
      this.fetchItemLog();
    },

    // 终止单个检查项
    async stopCheckItem(ip, itemId) {
      if (!ip || !itemId) {
        this.$message.error('参数错误：主机IP或检查项ID不能为空');
        return;
      }

      try {
        // 立即将检查项状态更新为"终止中"，提供用户视觉反馈
        const items = this.checkItemsMap[ip] || [];
        const targetItem = items.find(item => item.id === itemId);
        
        if (targetItem && targetItem.status === 'CHECKING') {
          targetItem.status = 'TERMINATING';
          targetItem.message = '正在终止检查...';
          // 更新本地缓存，确保UI立即显示变化
          this.$set(this.checkItemsMap, ip, [...items]);
        }

        // 调用后端API
        const res = await this.$axiosPost(global.API.stopCheckItem, { 
          clusterId: this.clusterId,
          ip: ip,
          itemId: itemId
        });
        
        if (res && res.code === 200) {
          this.$message.success('已终止检查项');
          
          // 延迟1秒后刷新列表，获取最新状态
          setTimeout(() => {
            this.refreshHostList();
          }, 1000);
        } else {
          this.$message.error(res.msg || '终止检查失败');
          this.refreshHostList(); // 还是需要刷新，恢复状态
        }
      } catch (error) {
        console.error('终止检查项失败:', error);
        this.$message.error('终止检查项失败，请检查网络连接');
        this.refreshHostList(); // 出错时也刷新，恢复状态
      }
    },

    // 关闭日志查看弹窗
    closeLogModal() {
      this.logVisible = false;
      this.stopAutoRefresh();
      this.currentLogIp = null;
      this.currentLogItemId = null;
      this.currentLogItemName = null;
      // 清理checkItem
      this.checkItem = null;
    },

    handleLogTypeChange() {
      // 当日志类型变化时，重新获取日志
      this.fetchItemLog();
      
      // 随着系统扩展，如果添加了新的日志类型，在这里不需要特殊处理
      // 只需要:
      // 1. 在上面的日志类型选择器中添加新的a-radio-button
      // 2. 确保后端OperationType枚举中添加了相应的类型
      // 3. 确保HostCheckServiceImpl.getCheckItemLogWithType方法支持新的日志类型
    },

    // 处理筛选变化
    handleFilterChange(filterData) {
      // 直接刷新日志以应用新的筛选条件
      this.fetchItemLog();
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
    
    formatHostsContent(content) {
      if (!content) return [];
      return content.split('\n');
    },
    
    splitIPAndHostname(line) {
      if (!line || line.trim() === '') return [''];
      return line.trim().split(/\s+/);
    },

    // 添加renderHostsFile方法，用于将hosts文件内容转换为带有语法高亮的HTML
    renderHostsFile(content) {
      if (!content) return '';
      
      const lines = this.formatHostsContent(content);
      let result = '';
      
      lines.forEach((line, index) => {
        let lineClass = 'hosts-line';
        let formattedLine = '';
        
        if (line.startsWith('#')) {
          // 注释行
          lineClass += ' comment';
          formattedLine = this.escapeHtml(line);
        } else if (line.trim() !== '') {
          // IP地址和主机名行
          lineClass += ' ip-entry';
          const parts = this.splitIPAndHostname(line);
          
          parts.forEach((part, partIndex) => {
            if (partIndex > 0) {
              // 主机名部分
              formattedLine += '<span class="hostname-part">' + this.escapeHtml(part) + '</span>';
            } else {
              // IP地址部分
              formattedLine += this.escapeHtml(part);
            }
            
            if (partIndex < parts.length - 1) {
              formattedLine += ' ';
            }
          });
        } else {
          // 空行
          formattedLine = '';
        }
        
        result += `<span class="${lineClass}">${formattedLine}</span>\n`;
      });
      
      return result;
    },

    // 添加formatDNSServers方法，用于格式化DNS服务器列表
    formatDNSServers(dnsList) {
      if (!dnsList || !dnsList.length) return '<div class="dns-empty">未配置DNS服务器</div>';
      
      let result = '';
      
      dnsList.forEach((dns, index) => {
        result += `<div class="dns-server-entry">
          <span class="dns-icon">${index+1}</span>
          <span class="dns-ip">${this.escapeHtml(dns)}</span>
        </div>`;
      });
      
      return result;
    },

    // 添加escapeHtml方法到methods
    escapeHtml(unsafe) {
      if (!unsafe) return '';
      return unsafe
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    },

    // 渲染DNS服务器列表
    renderDnsServers (dnsServers) {
      if (!dnsServers || dnsServers.length === 0) {
        return h('div', { class: 'dns-empty' }, ['没有配置DNS服务器']);
      }
      
      return dnsServers.map((dns, index) => {
        return h('div', { 
          class: 'dns-server-entry',
          key: `dns-${index}`
        }, [
          h('div', { class: 'dns-icon' }, [`#${index+1}`]),
          h('div', { class: 'dns-ip' }, [dns])
        ]);
      });
    },
    
    // 渲染hosts文件内容，带有语法高亮
    renderHostsFile (hostsContent) {
      if (!hostsContent) {
        return '未能获取hosts文件内容';
      }
      
      // 对hosts文件进行HTML转义，防止XSS攻击
      const escapedContent = this.escapeHtml(hostsContent);
      
      // 将hosts文件内容按行分割
      const lines = escapedContent.split('\n');
      const result = [];
      
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        let lineClass = 'hosts-line';
        let lineContent = line;
        
        // 处理注释行
        if (line.trim().startsWith('#')) {
          lineClass += ' comment';
        } 
        // 处理IP行
        else if (/^\s*\d+\.\d+\.\d+\.\d+/.test(line)) {
          lineClass += ' ip-entry';
          // 将主机名部分用span包裹以便应用样式
          lineContent = line.replace(/(\s+\S+)(?=\s|$)/g, '<span class="hostname-part">$1</span>');
        }
        
        result.push(h('div', { 
          class: lineClass,
          key: `line-${i}`,
          domProps: {
            innerHTML: lineContent
          } 
        }));
      }
      
      return result;
    },
    
    // HTML转义函数，防止XSS攻击
    escapeHtml (unsafe) {
      if (!unsafe) return '';
      return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
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
  filters: {
    // HTML转义函数，用于防止XSS攻击
    escapeHtml(unsafe) {
      if (!unsafe) return '';
      return unsafe
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    }
  },
};
</script>

<style lang="less" scoped>
@import '../../assets/less/theme.less';

// Apple风格变量定义
@apple-blue: #007AFF;
@apple-blue-hover: #0062CC;
@apple-red: #FF3B30;
@apple-red-hover: #D82D22;
@apple-green: #34C759;
@apple-orange: #FF9500;
@apple-purple: #AF52DE;
@apple-gray-light: #F2F2F7;
@apple-gray: #AEAEB2;
@apple-gray-dark: #8E8E93;
@apple-black: #1D1D1F;
@apple-white: #FFFFFF;

// 系统主题变量
@primary-color: #2872e0;
@primary-1: #f0f8ff;
@primary-2: #d1eaff;
@primary-3: #a8d4ff;
@primary-4: #7db7fa;
@primary-5: #5194ed;
@primary-6: #2872e0;
@primary-7: #1854ba;
@primary-8: #0c3994;
@primary-9: #03236e;
@primary-10: #011447;
@info-color: #2872e0;
@success-color: #52c41a;
@warning-color: #faad14;
@error-color: #f5222f;
@alert-info-bg-color: #f0f8ff;
@alert-info-border-color: #a8d4ff;
@alert-success-bg-color: #f6ffed;
@alert-success-border-color: #b7eb8f;
@alert-warning-bg-color: #fffbe6;
@alert-warning-border-color: #ffe58f;
@alert-error-bg-color: #fff1f0;
@alert-error-border-color: #ffa19e;
@processing-color: #2872e0;
@menu-dark-submenu-bg: #030810;
@layout-header-background: #071326;
@layout-trigger-background: #0b1f3c;
@btn-danger-bg: #ff4d52;
@btn-danger-border: #ff4d52;
@layout-body-background: #f0f2f5;
@body-background: #fff;
@component-background: #fff;
@heading-color: rgba(0, 0, 0, 0.85);
@text-color: rgba(0, 0, 0, 0.65);
@text-color-inverse: #fff;
@text-color-secondary: rgba(0, 0, 0, 0.45);
@shadow-color: rgba(0, 0, 0, 0.15);
@border-color-split: #f0f0f0;
@background-color-light: #fafafa;
@background-color-base: #f5f5f5;
@table-selected-row-bg: #fafafa;
@table-expanded-row-bg: #fbfbfb;
@checkbox-check-color: #fff;
@disabled-color: rgba(0, 0, 0, 0.25);
@menu-dark-color: rgba(254, 254, 254, 0.65);
@menu-dark-highlight-color: #fefefe;
@menu-dark-arrow-color: #fefefe;
@btn-primary-color: #fff;

// Apple风格混合函数
.apple-font() {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.steps-container {
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
  
  // 基础样式
  .os-tooltip {
    max-width: none !important;
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
  }
  
  // 头部区域
  .os-detail-header {
    padding: 20px;
    display: flex;
    align-items: center;
    background: linear-gradient(135deg, #F5F5F7, #E5E5EA);
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    
    .os-detail-icon-container {
      margin-right: 20px;
      
      img {
        width: 48px;
        height: 48px;
        padding: 6px;
        border-radius: 12px;
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
        background-color: white;
      }
    }
    
    .os-detail-title-container {
      color: #1D1D1F;
      
      .os-detail-title {
        font-size: 1.3rem;
        font-weight: 600;
        margin: 0 0 4px 0;
      }
      
      .os-detail-subtitle {
        font-size: 0.9rem;
        color: #86868B;
        margin-bottom: 2px;
      }
    }
  }
  
  // 内容区域
  .os-detail-content {
    padding: 16px 20px;
  }
  
  // 卡片样式
  .os-detail-card {
    margin-bottom: 16px;
    border-radius: 12px;
    background-color: #ffffff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    border: 1px solid rgba(0, 0, 0, 0.06);
    overflow: hidden;
    
    .os-detail-card-header {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      background-color: #F5F5F7;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      font-weight: 600;
      font-size: 14px;
      color: #1D1D1F;
    }
  }
  
  // 信息行样式
  .os-detail-info-row {
    display: flex;
    padding: 12px 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.04);
    
    &:last-child {
      border-bottom: none;
    }
    
    .os-detail-info-icon-container {
      margin-right: 16px;
      display: flex;
      align-items: center;
      
      .os-detail-info-icon {
        width: 36px;
        height: 36px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        i {
          font-size: 20px;
        }
        
        &.cpu {
          background-color: rgba(0, 122, 255, 0.1);
          color: #007AFF;
        }
        
        &.memory {
          background-color: rgba(255, 149, 0, 0.1);
          color: #FF9500;
        }
        
        &.storage {
          background-color: rgba(52, 199, 89, 0.1);
          color: #34C759;
        }
        
        &.swap {
          background-color: rgba(175, 82, 222, 0.1);
          color: #AF52DE;
        }
        
        &.gpu {
          background-color: rgba(255, 59, 48, 0.1);
          color: #FF3B30;
        }
      }
    }
    
    .os-detail-info-content {
      flex: 1;
      
      .os-detail-info-label {
        display: flex;
        align-items: center;
        font-size: 14px;
        color: #86868B;
        margin-bottom: 6px;
      }
      
      .os-detail-info-value {
        font-size: 14px;
        color: #1D1D1F;
        word-break: break-word;
        
        &.loading {
          display: flex;
          align-items: center;
          color: #007AFF;
          
          .loading-animation {
            width: 12px;
            height: 12px;
            border: 2px solid rgba(0, 122, 255, 0.3);
            border-top: 2px solid #007AFF;
            border-radius: 50%;
            margin-right: 8px;
            animation: spin 1s linear infinite;
          }
        }
        
        &.error {
          color: #FF3B30;
        }
        
        &.waiting {
          color: #FAAD14;
        }
      }
    }
  }
  
  // 信息区样式
  .info-section {
    margin-bottom: 20px;
    
    .section-title {
      font-size: 14px;
      font-weight: 600;
      color: #1D1D1F;
      margin-bottom: 12px;
      display: flex;
      align-items: center;
      
      .icon {
        margin-right: 6px;
        color: #007AFF;
        font-size: 16px;
      }
    }
    
    .info-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 0;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      
      &:last-child {
        border-bottom: none;
      }
      
      .info-label {
        color: #86868B;
        font-size: 13px;
        flex: 0 0 40%;
      }
      
      .info-value {
        color: #1D1D1F;
        font-size: 13px;
        font-weight: 500;
        text-align: right;
        word-break: break-word;
        flex: 0 0 60%;
      }
      
      .progress-container {
        width: 100%;
        margin-top: 6px;
        
        .progress-bar {
          height: 6px;
          width: 100%;
          background-color: #F5F5F7;
          border-radius: 3px;
          overflow: hidden;
          
          .progress-fill {
            height: 100%;
            border-radius: 3px;
            transition: width 0.3s ease;
            
            &.normal {
              background-color: #34C759;
            }
            
            &.warning {
              background-color: #FF9500;
            }
            
            &.danger {
              background-color: #FF3B30;
            }
          }
        }
        
        .progress-text {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: #86868B;
          margin-top: 4px;
        }
      }
    }
  }
  
  // 加载相关样式
  .os-detail-info-value.loading {
    display: flex;
    align-items: center;
    color: #8E8E93;
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
  
  // GPU信息样式
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

// 主机名悬浮卡片样式
.hostname-tooltip {
  max-width: none !important;
  
  .ant-tooltip-inner {
    background-color: transparent !important;
    padding: 0 !important;
    color: inherit !important;
    box-shadow: none !important;
  }
  
  .ant-tooltip-arrow {
    display: none !important;
  }
}

// 添加CSS变量定义，在<style lang="less" scoped>中的最开始添加
:root {
  /* 苹果风格的设计变量 */
  --card-radius: 12px;
  --card-radius-small: 8px;
  --transition-curve: cubic-bezier(0.28, 0.62, 0.35, 1);
  
  /* 浅色模式 */
  --bg-color: #F5F5F7;
  --card-bg: #FFFFFF;
  --text-primary: #1D1D1F;
  --text-secondary: #86868B;
  --accent-color: #007AFF;
  --border-color: rgba(0, 0, 0, 0.05);
  --card-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 12px 32px rgba(0, 0, 0, 0.05);
  --card-shadow-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
  
  /* 状态颜色 */
  --success-color: #34C759;
  --warning-color: #FF9500;
  --error-color: #FF3B30;
  --info-color: #007AFF;
}

// 全面重新设计主机名悬浮卡片
.hostname-detail-tooltip {
  width: 420px;
  padding: 0;
  border-radius: @apple-card-radius;
  background-color: @apple-card-bg;
  box-shadow: @apple-card-shadow;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', sans-serif;
  animation: fadeIn 0.3s @apple-transition-curve;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
  z-index: 1001 !important; // 确保悬浮卡片在最上层
  opacity: 1 !important; // 确保不透明
  visibility: visible !important; // 确保可见
  
  /* 增加3D转换效果 */
  transform-origin: center center;
  perspective: 1000px;
  
  &:hover {
    box-shadow: @apple-card-shadow-hover;
    transform: translateY(-2px) scale(1.01);
  }
  
  &::-webkit-scrollbar {
    width: 8px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.1);
    border-radius: 20px;
    border: 2px solid transparent;
    background-clip: content-box;
  }
  
  &::-webkit-scrollbar-thumb:hover {
    background-color: rgba(0, 0, 0, 0.2);
    border: 2px solid transparent;
    background-clip: content-box;
  }
}

/* 毛玻璃效果标题栏 */
.hostname-detail-title {
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  font-weight: 600;
  font-size: 15px;
  color: @apple-text-primary;
  border-bottom: 1px solid @apple-border-color;
  display: flex;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
  transition: all 0.3s @apple-transition-curve;
  
  .section-icon {
    width: 32px;
    height: 32px;
    border-radius: @apple-card-radius-small;
    margin-right: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
    transition: transform 0.3s @apple-transition-curve;
    
    &:hover {
      transform: scale(1.05);
    }
    
    &.hostname-icon {
      background: linear-gradient(135deg, #34C759, #30B350);
      color: white;
    }
    
    &.network-icon {
      background: linear-gradient(135deg, #007AFF, #0063D1);
      color: white;
    }
    
    &.hosts-icon {
      background: linear-gradient(135deg, #FF9500, #E68600);
      color: white;
    }
    
    .anticon {
      font-size: 18px;
    }
  }
}

.hostname-detail-content {
  padding: 16px 20px;
  background-color: @apple-card-bg;
  border-bottom: 1px solid @apple-border-color;
}

.hostname-detail-label {
  font-size: 13px;
  color: @apple-text-secondary;
  margin-bottom: 6px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

/* 美化DNS服务器显示，更贴近苹果风格 */
.hostname-detail-value {
  &.dns-servers {
    color: @apple-text-primary;
    white-space: pre-line;
    font-family: 'SF Mono', Menlo, Monaco, monospace;
    background-color: @apple-bg-color;
    padding: 16px;
    border-radius: @apple-card-radius-small;
    font-size: 13px;
    line-height: 1.5;
    margin: 8px 0;
    
    .dns-empty {
      color: @apple-text-secondary;
      text-align: center;
      padding: 12px;
      font-style: italic;
      background-color: rgba(255, 255, 255, 0.6);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      
      &::before {
        content: "!";
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 18px;
        height: 18px;
        background-color: rgba(0, 0, 0, 0.08);
        border-radius: 50%;
        margin-right: 8px;
        font-style: normal;
        font-weight: 600;
        color: @apple-text-secondary;
      }
    }
    
    .dns-server-entry {
      display: flex;
      align-items: center;
      margin-bottom: 10px;
      background-color: rgba(255, 255, 255, 0.8);
      padding: 10px 12px;
      border-radius: 8px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      transition: all 0.2s @apple-transition-curve;
      
      &:hover {
        transform: translateX(4px);
        background-color: rgba(255, 255, 255, 0.95);
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
      }
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .dns-icon {
        width: 22px;
        height: 22px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background: linear-gradient(135deg, #007AFF, #0063D1);
        margin-right: 12px;
        color: white;
        font-size: 11px;
        font-weight: 600;
        box-shadow: 0 1px 2px rgba(0, 122, 255, 0.3);
      }
      
      .dns-ip {
        font-weight: 500;
        color: @apple-accent-color;
        font-size: 14px;
        letter-spacing: 0.3px;
      }
    }
  }
}

/* 美化hosts文件显示效果，更贴近苹果风格，淡化注释 */
.hostname-detail-hosts-file {
  margin: 8px 0;
  padding: 20px;
  background-color: @apple-bg-color;
  border-radius: @apple-card-radius-small;
  font-family: 'SF Mono', Menlo, Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: @apple-text-primary;
  overflow-x: auto;
  max-height: 240px;
  white-space: pre-wrap;
  counter-reset: line;
  position: relative;
  
  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.1);
    border-radius: 10px;
  }
  
  /* 添加代码编辑器风格的行号和背景 */
  &::before {
    content: "";
    position: absolute;
    left: 0;
    top: 0;
    width: 3.5em;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.02);
    border-right: 1px solid rgba(0, 0, 0, 0.05);
    z-index: 1;
  }
  
  .hosts-line {
    display: block;
    position: relative;
    padding-left: 4.5em;
    transition: background-color 0.2s @apple-transition-curve;
    
    &:hover {
      background-color: rgba(0, 0, 0, 0.03);
    }
    
    &::before {
      counter-increment: line;
      content: counter(line);
      position: absolute;
      left: 0;
      width: 3.5em;
      text-align: right;
      color: @apple-text-secondary;
      font-size: 12px;
      padding-right: 1.5em;
      z-index: 2;
    }
    
    &.comment {
      color: #C7C7CC; // 更淡的灰色，符合苹果设计风格，淡化注释
      font-style: italic;
    }
    
    &.ip-entry {
      color: @apple-accent-color; // 苹果蓝色
      
      .hostname-part {
        color: @apple-warning-color; // 苹果橙色
        font-weight: 500;
      }
    }
  }
}

/* 添加卡片出现的动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 主机名卡片内的项目样式 */
.hostname-detail-item {
  margin-bottom: 12px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

/* 在展开详情区域的卡片样式 */
.detail-card.hostname-card {
  border-radius: @apple-card-radius;
  background-color: @apple-card-bg;
  box-shadow: @apple-card-shadow;
  overflow: hidden;
  transition: all 0.3s @apple-transition-curve;
  
  &:hover {
    box-shadow: @apple-card-shadow-hover;
    transform: translateY(-2px);
  }
  
  .card-header {
    padding: 16px;
    background: linear-gradient(135deg, #F5F5F7, #E5E5EA);
    border-bottom: 1px solid @apple-border-color;
    
    .card-title {
      display: flex;
      align-items: center;
      font-weight: 600;
      color: @apple-text-primary;
      
      .anticon {
        margin-right: 8px;
      }
    }
  }
  
  .card-content {
    padding: 16px;
    
    .content-item {
      display: flex;
      flex-direction: column;
      
      .item-label {
        font-size: 13px;
        color: @apple-text-secondary;
        margin-bottom: 4px;
      }
      
      .item-value {
        font-size: 15px;
        color: @apple-text-primary;
        font-weight: 500;
      }
    }
  }
}

// 动画相关
@keyframes osFadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

@keyframes shimmer {
  to { background-position: -100% 0; }
}

// 添加苹果风格的设计变量
@apple-card-radius: 12px;
@apple-card-radius-small: 8px;
@apple-transition-curve: cubic-bezier(0.28, 0.62, 0.35, 1);
@apple-bg-color: #F5F5F7;
@apple-card-bg: #FFFFFF;
@apple-text-primary: #1D1D1F;
@apple-text-secondary: #86868B;
@apple-accent-color: #007AFF;
@apple-border-color: rgba(0, 0, 0, 0.05);
@apple-card-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 12px 32px rgba(0, 0, 0, 0.05);
@apple-card-shadow-hover: 0 8px 32px rgba(0, 0, 0, 0.12);
@apple-success-color: #34C759;
@apple-warning-color: #FF9500;
@apple-error-color: #FF3B30;
@apple-info-color: #007AFF;

// 添加淡入动画
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
