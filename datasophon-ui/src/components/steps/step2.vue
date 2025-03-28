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
          width: 70,
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
            
            // 检查osInfoStatus状态，显示加载动画
            // 当osInfoStatus为'loading'或osInfo为null时都显示加载动画
            if (row.osInfoStatus === 'loading' || row.osInfo === null || row.osInfoStatus === null) {
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
                }, ['正在优雅检索主机名信息...'])
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
                    width: '90%',
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
                      zIndex: 2
                    }
                  }, ['正在检索主机名...'])
                ])
              ]);
            } else if (row.osInfoStatus === 'error') {
              // 错误状态显示
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
                '系统信息获取失败'
              ]);
            }
            
            // 当主机名为null时也显示加载状态
            if (!row.hostname) {
              // 苹果风格的骨架屏加载动画
              return h('div', {
                class: 'os-info-loading',
                style: {
                  position: 'relative',
                  height: '24px',
                  width: '90%',
                  borderRadius: '4px',
                  backgroundColor: 'rgba(240, 240, 240, 0.8)',
                  overflow: 'hidden'
                }
              });
            }
            
            // 正常显示主机名，添加悬浮卡片
            const tooltipContent = h('div', { class: 'hostname-detail-tooltip' }, [
              // 主机名信息部分
              h('div', { class: 'hostname-detail-section' }, [
                h('div', { class: 'hostname-detail-title' }, ['主机基本信息']),
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
              
              // DNS和hosts文件信息部分
              h('div', { class: 'hostname-detail-section' }, [
                h('div', { class: 'hostname-detail-title' }, ['DNS和Hosts配置']),
                // DNS信息
                h('div', { class: 'hostname-detail-block' }, [
                  h('div', { class: 'hostname-detail-subtitle' }, ['DNS服务器']),
                  row.osInfo && row.osInfo.dnsServers ? 
                    h('div', { class: 'hostname-detail-content dns-servers' }, [
                      row.osInfo.dnsServers
                    ]) : 
                    h('div', { class: 'hostname-detail-empty' }, ['未获取到DNS服务器信息'])
                ]),
                
                // Hosts文件信息
                h('div', { class: 'hostname-detail-block' }, [
                  h('div', { class: 'hostname-detail-subtitle' }, ['Hosts文件']),
                  row.hostsFileStatus === 'loading' || row.hostsFileStatus === null ? 
                    h('div', { class: 'hostname-detail-loading' }, [
                      h('div', { class: 'loading-animation' }),
                      h('span', { class: 'loading-text' }, ['正在读取hosts文件...'])
                    ]) : 
                    row.hostsFileStatus === 'error' ?
                      h('div', { class: 'hostname-detail-error' }, [
                        h('a-icon', { props: { type: 'warning' }, style: { marginRight: '6px' } }),
                        '读取hosts文件失败'
                      ]) :
                      h('pre', { 
                        class: 'hostname-detail-hosts-content',
                        style: {
                          maxHeight: '200px',
                          overflow: 'auto',
                          margin: '8px 0',
                          padding: '12px',
                          backgroundColor: '#F5F5F7',
                          borderRadius: '8px',
                          fontFamily: 'monospace',
                          fontSize: '12px',
                          lineHeight: '1.5',
                          color: '#1D1D1F',
                          border: '1px solid #E5E5EA'
                        }
                      }, [row.hostsFile || '# 暂无hosts文件内容'])
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
                style: {
                  cursor: 'pointer',
                  color: '#007AFF',
                  fontWeight: '500',
                  display: 'inline-flex',
                  alignItems: 'center'
                }
              }, [
                row.hostname,
                h('a-icon', {
                  props: { type: 'info-circle' },
                  style: { 
                    marginLeft: '6px', 
                    fontSize: '14px',
                    color: '#8E8E93',
                    opacity: 0.7
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
          width: 120  // 缩小IP列宽度
        },
        {
          title: "操作系统",
          key: "osType",
          dataIndex: "osType",
          width: "25%",  // 增加操作系统列宽度
          customRender: (text, row) => {
            const h = this.$createElement;
            
            // 检查osInfoStatus状态，显示加载动画
            // 当osInfoStatus为'loading'或osInfo为null时都显示加载动画
            if (row.osInfoStatus === 'loading' || row.osInfo === null || row.osInfoStatus === null) {
              // 如果SSH连接失败，则显示错误状态而不是加载状态
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
                }, ['正在优雅地检索操作系统信息...'])
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
            
            // 创建详细的操作系统信息弹出框
            const osDetailContent = hasOsInfo ? h('div', { class: 'os-detail-popup' }, [
              // 标题区域
              h('div', { class: 'os-detail-header' }, [
                h('div', { class: 'os-detail-icon-container' }, [
                  h('img', {
                    attrs: {
                      src: getOsIconPath(osType),
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
                // 系统概览卡片
                h('div', { class: 'os-detail-card' }, [
                  h('div', { class: 'os-detail-card-header' }, [
                    h('i', { class: 'anticon anticon-dashboard', style: { marginRight: '8px', color: '#007AFF' }}),
                    h('span', {}, ['系统概览'])
                  ]),
                  
                  // CPU信息
                  h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { class: 'os-detail-info-icon cpu' }, [
                        h('i', { class: 'anticon anticon-api' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, ['处理器']),
                      row.osInfo.hardwareCollectionStatus === 'collecting' && (!row.osInfo.lastUpdatedItem || row.osInfo.lastUpdatedItem !== 'cpuInfo') ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', { class: 'loading-text' }, ['正在收集...'])
                        ]) : 
                        h('div', { class: 'os-detail-info-value' }, [row.osInfo.cpuInfo || '未知']),
                      h('div', { class: 'os-detail-info-subvalue' }, [
                        row.osInfo.hardwareCollectionStatus === 'collecting' && (!row.osInfo.lastUpdatedItem || row.osInfo.lastUpdatedItem !== 'cpuCores') ?
                          h('span', { class: 'loading-text-simple' }, ['收集中...']) :
                          (row.osInfo.cpuCores ? 
                            (row.osInfo.cpuCount > 1 ? 
                              `${row.osInfo.cpuCount} 颗 CPU (${row.osInfo.cpuCores} 核 / ${row.osInfo.cpuLogicalCores || row.osInfo.cpuCores * row.osInfo.cpuThreadsPerCore} 线程)` : 
                              `${row.osInfo.cpuCores} 核 / ${row.osInfo.cpuLogicalCores || row.osInfo.cpuCores * row.osInfo.cpuThreadsPerCore} 线程`
                            ) : '')
                      ])
                    ])
                  ]),
                  
                  // 内存信息
                  h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { class: 'os-detail-info-icon memory' }, [
                        h('i', { class: 'anticon anticon-database' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, ['内存']),
                      row.osInfo.hardwareCollectionStatus === 'collecting' && (!row.osInfo.lastUpdatedItem || row.osInfo.lastUpdatedItem !== 'memoryInfo') ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', { class: 'loading-text' }, ['正在收集...'])
                        ]) : 
                        h('div', { class: 'os-detail-info-value with-progress' }, [
                          h('span', {}, [row.osInfo.totalMemory ? `${row.osInfo.totalMemory.toFixed(1)} GB` : '未知']),
                          row.osInfo.totalMemory && row.osInfo.availableMemory ? 
                            h('div', { class: 'os-detail-progress-container' }, [
                              h('div', { 
                                class: 'os-detail-progress-bar',
                                style: {
                                  width: `${((row.osInfo.totalMemory - row.osInfo.availableMemory) / row.osInfo.totalMemory * 100).toFixed(0)}%`,
                                  backgroundColor: ((row.osInfo.totalMemory - row.osInfo.availableMemory) / row.osInfo.totalMemory > 0.8) ? '#FF3B30' : '#34C759'
                                }
                              })
                            ]) : null
                        ]),
                      h('div', { class: 'os-detail-info-subvalue' }, [
                        row.osInfo.hardwareCollectionStatus === 'collecting' && (!row.osInfo.lastUpdatedItem || row.osInfo.lastUpdatedItem !== 'memoryInfo') ?
                          h('span', { class: 'loading-text-simple' }, ['收集中...']) :
                          (row.osInfo.totalMemory && row.osInfo.availableMemory ? 
                            `可用: ${row.osInfo.availableMemory.toFixed(1)} GB (${((row.osInfo.availableMemory / row.osInfo.totalMemory) * 100).toFixed(1)}%)` : 
                            '')
                      ])
                    ])
                  ]),
                  
                  // 存储信息
                  h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { class: 'os-detail-info-icon storage' }, [
                        h('i', { class: 'anticon anticon-hdd' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, ['存储']),
                      row.osInfo.hardwareCollectionStatus === 'collecting' && (!row.osInfo.lastUpdatedItem || row.osInfo.lastUpdatedItem !== 'diskInfo') ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', { class: 'loading-text' }, ['正在收集...'])
                        ]) : 
                        h('div', { class: 'os-detail-info-value with-progress' }, [
                          h('span', {}, [
                            row.osInfo.totalDisk ? 
                              (typeof row.osInfo.totalDisk === 'number' ? 
                                (row.osInfo.totalDisk >= 1099511627776 ? `${((row.osInfo.totalDisk / 1099511627776)).toFixed(1)} TB` : `${((row.osInfo.totalDisk / 1073741824)).toFixed(1)} GB`) :
                                (row.osInfo.totalDisk >= 1024 ? `${(row.osInfo.totalDisk / 1024).toFixed(1)} TB` : `${row.osInfo.totalDisk} GB`)
                              ) : 
                              '未知'
                          ]),
                          row.osInfo.totalDisk && row.osInfo.availableDisk ? 
                            h('div', { class: 'os-detail-progress-container' }, [
                              h('div', { 
                                class: 'os-detail-progress-bar',
                                style: {
                                  width: `${((row.osInfo.totalDisk - row.osInfo.availableDisk) / row.osInfo.totalDisk * 100).toFixed(0)}%`,
                                  backgroundColor: ((row.osInfo.totalDisk - row.osInfo.availableDisk) / row.osInfo.totalDisk > 0.9) ? '#FF3B30' : '#34C759'
                                }
                              })
                            ]) : null
                        ]),
                      h('div', { class: 'os-detail-info-subvalue' }, [
                        row.osInfo.hardwareCollectionStatus === 'collecting' && (!row.osInfo.lastUpdatedItem || row.osInfo.lastUpdatedItem !== 'diskInfo') ?
                          h('span', { class: 'loading-text-simple' }, ['收集中...']) :
                          (row.osInfo.totalDisk && row.osInfo.availableDisk ? 
                            `可用: ${
                              typeof row.osInfo.availableDisk === 'number' ?
                                (row.osInfo.availableDisk >= 1099511627776 ? ((row.osInfo.availableDisk / 1099511627776)).toFixed(1) + ' TB' : ((row.osInfo.availableDisk / 1073741824)).toFixed(1) + ' GB') :
                                (row.osInfo.availableDisk >= 1024 ? (row.osInfo.availableDisk / 1024).toFixed(1) + ' TB' : row.osInfo.availableDisk + ' GB')
                            } (${((row.osInfo.availableDisk / row.osInfo.totalDisk) * 100).toFixed(1)}%)` : 
                            '')
                      ])
                    ])
                  ]),
                  
                  // 交换空间信息
                  row.osInfo.totalSwap > 0 ? h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { class: 'os-detail-info-icon swap' }, [
                        h('i', { class: 'anticon anticon-swap' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, ['交换空间']),
                      row.osInfo.hardwareCollectionStatus === 'collecting' && (!row.osInfo.lastUpdatedItem || row.osInfo.lastUpdatedItem !== 'swapInfo') ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', { class: 'loading-text' }, ['正在收集...'])
                        ]) : 
                        h('div', { class: 'os-detail-info-value with-progress' }, [
                          h('span', {}, [
                            row.osInfo.totalSwap ? 
                              (typeof row.osInfo.totalSwap === 'number' ?
                                `${((row.osInfo.totalSwap / 1073741824)).toFixed(1)} GB` :
                                `${row.osInfo.totalSwap} GB`
                              ) : '未知'
                          ]),
                          row.osInfo.totalSwap && row.osInfo.availableSwap ? 
                            h('div', { class: 'os-detail-progress-container' }, [
                              h('div', { 
                                class: 'os-detail-progress-bar',
                                style: {
                                  width: `${((row.osInfo.totalSwap - row.osInfo.availableSwap) / row.osInfo.totalSwap * 100).toFixed(0)}%`,
                                  backgroundColor: ((row.osInfo.totalSwap - row.osInfo.availableSwap) / row.osInfo.totalSwap > 0.8) ? '#FF3B30' : '#34C759'
                                }
                              })
                            ]) : null
                        ]),
                      h('div', { class: 'os-detail-info-subvalue' }, [
                        row.osInfo.hardwareCollectionStatus === 'collecting' && (!row.osInfo.lastUpdatedItem || row.osInfo.lastUpdatedItem !== 'swapInfo') ?
                          h('span', { class: 'loading-text-simple' }, ['收集中...']) :
                          (row.osInfo.totalSwap && row.osInfo.availableSwap ? 
                            `可用: ${
                              typeof row.osInfo.availableSwap === 'number' ?
                                `${((row.osInfo.availableSwap / 1073741824)).toFixed(1)} GB` :
                                `${row.osInfo.availableSwap} GB`
                            } (${((row.osInfo.availableSwap / row.osInfo.totalSwap) * 100).toFixed(1)}%)` : 
                            '')
                      ])
                    ])
                  ]) : null,
                  
                  // 显卡信息（如果有）
                  row.osInfo.gpuInfo ? h('div', { class: 'os-detail-info-row' }, [
                    h('div', { class: 'os-detail-info-icon-container' }, [
                      h('div', { class: 'os-detail-info-icon gpu' }, [
                        h('i', { class: 'anticon anticon-appstore' })
                      ])
                    ]),
                    h('div', { class: 'os-detail-info-content' }, [
                      h('div', { class: 'os-detail-info-label' }, ['显卡']),
                      row.osInfo.hardwareCollectionStatus === 'collecting' && (!row.osInfo.lastUpdatedItem || row.osInfo.lastUpdatedItem !== 'gpuInfo') ? 
                        h('div', { class: 'os-detail-info-value loading' }, [
                          h('div', { class: 'loading-animation' }),
                          h('span', { class: 'loading-text' }, ['正在收集...'])
                        ]) : 
                        h('div', { class: 'os-detail-info-value' }, [row.osInfo.gpuInfo || '未知']),
                      row.osInfo.gpuMemory > 0 ? h('div', { class: 'gpu-memory-info' }, [
                        `显存: ${row.osInfo.gpuMemory.toFixed(1)} GB`
                      ]) : null
                    ])
                  ]) : null,
                ]),
                
                // 系统详情
                h('div', { class: 'os-detail-card' }, [
                  h('div', { class: 'os-detail-card-header' }, [
                    h('i', { class: 'anticon anticon-info-circle', style: { marginRight: '8px', color: '#5856D6' }}),
                    h('span', {}, ['系统详情'])
                  ]),
                  
                  h('div', { class: 'os-detail-table' }, [
                    h('div', { class: 'os-detail-table-row' }, [
                      h('div', { class: 'os-detail-table-cell label' }, ['主机名']),
                      h('div', { class: 'os-detail-table-cell value' }, [
                        row.hostname ? row.hostname : h('span', { style: { color: '#8E8E93', fontStyle: 'italic' } }, ['正在获取...'])
                      ])
                    ]),
                    h('div', { class: 'os-detail-table-row' }, [
                      h('div', { class: 'os-detail-table-cell label' }, ['完整域名']),
                      h('div', { class: 'os-detail-table-cell value' }, [
                        row.fqdn ? row.fqdn : h('span', { style: { color: '#8E8E93', fontStyle: 'italic' } }, ['正在获取...'])
                      ])
                    ]),
                    h('div', { class: 'os-detail-table-row' }, [
                      h('div', { class: 'os-detail-table-cell label' }, ['DNS服务器']),
                      h('div', { class: 'os-detail-table-cell value' }, [
                        row.osInfo.dnsServers ? row.osInfo.dnsServers : h('span', { style: { color: '#8E8E93', fontStyle: 'italic' } }, ['正在获取...'])
                      ])
                    ]),
                    h('div', { class: 'os-detail-table-row' }, [
                      h('div', { class: 'os-detail-table-cell label' }, ['发行版']),
                      h('div', { class: 'os-detail-table-cell value' }, [row.osInfo.distribution || '-'])
                    ]),
                    h('div', { class: 'os-detail-table-row' }, [
                      h('div', { class: 'os-detail-table-cell label' }, ['发行版ID']),
                      h('div', { class: 'os-detail-table-cell value' }, [row.osInfo.distributionId || '-'])
                    ]),
                    h('div', { class: 'os-detail-table-row' }, [
                      h('div', { class: 'os-detail-table-cell label' }, ['系统版本']),
                      h('div', { class: 'os-detail-table-cell value' }, [row.osInfo.versionId || '-'])
                    ]),
                    h('div', { class: 'os-detail-table-row' }, [
                      h('div', { class: 'os-detail-table-cell label' }, ['系统架构']),
                      h('div', { class: 'os-detail-table-cell value' }, [row.osInfo.architecture || '-'])
                    ])
                  ])
                ])
              ]),
              
              // 底部信息区域
              h('div', { class: 'os-detail-footer' }, [
                h('span', { class: 'os-detail-footer-text' }, [`IP: ${row.ip || '-'}`])
              ])
            ]) : h('div', { 
              class: 'os-detail-popup',
              style: { 
                padding: '16px',
                textAlign: 'center',
                color: '#8E8E93'
              }
            }, [
              h('div', { class: 'os-detail-no-info' }, [
                h('i', { 
                  class: 'anticon anticon-info-circle',
                  style: { fontSize: '32px', marginBottom: '16px', color: '#8E8E93' }
                }),
                h('div', { class: 'os-detail-no-info-text' }, ['暂无详细信息']),
                h('div', { class: 'os-detail-no-info-subtext' }, ['尝试重新检查主机以获取系统信息'])
              ])
            ]);
            
            return h('div', { class: 'os-info' }, [
              h('a-tooltip', {
                props: {
                  placement: 'right',
                  arrowPointAtCenter: true,
                  overlayClassName: 'os-tooltip',
                  getPopupContainer: () => document.body
                }
              }, [
                // 简要信息(触发区域)
                h('span', { 
                  slot: 'title',
                  class: 'os-detail-tooltip'
                }, [osDetailContent]),
                
                // 显示的内容
                h('span', { 
                  class: 'os-type',
                  style: {
                    display: 'flex',
                    alignItems: 'center',
                    cursor: hasOsInfo ? 'pointer' : 'default'
                  }
                }, [
                  h('img', {
                    attrs: {
                      src: iconPath,
                      alt: osType,
                      width: '20',
                      height: '20'
                    },
                    class: 'os-logo',
                    style: { 
                      marginRight: '8px',
                      verticalAlign: 'middle'
                    }
                  }),
                  h('span', {
                    style: {
                      color: color
                    }
                  }, [
                    osType === '-' ? '-' : (osType + (osVersion ? ' ' + osVersion : ''))
                  ])
                ])
              ])
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

// 日志模态框内容样式
.log-modal {
  .log-container {
    display: flex;
    flex-direction: column;
    height: 70vh;
    
    .log-header {
      padding: 16px 24px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      background-color: @apple-gray-light;
      
      .header-section {
        margin-bottom: 12px;
        
        &:last-child {
          margin-bottom: 0;
        }
      }
      
      .refresh-options {
    display: flex;
        gap: 10px;
        
        .refresh-btn, .auto-refresh-btn {
          height: 36px;
          padding: 0 16px;
          font-size: 0.9rem;
          font-weight: 500;
          border-radius: 18px;
      display: flex;
      align-items: center;
          
          .anticon {
            margin-right: 6px;
          }
        }
      }
      
      .filter-area {
    display: flex;
        flex-wrap: wrap;
        gap: 16px;
        align-items: center;
        margin-top: 16px;
        
        .log-type-selector {
      display: flex;
      align-items: center;
          
          .filter-title {
            margin-right: 8px;
            font-weight: 500;
            color: @apple-black;
          }
        }
      }
      
      .combined-filter-status {
        margin-top: 16px;
        padding: 10px 16px;
        background-color: rgba(0, 0, 0, 0.02);
      border-radius: 8px;
        
        .filter-description {
          font-size: 0.9rem;
          color: @apple-gray;
          
          .highlight {
            color: @apple-black;
    font-weight: 500;
          }
        }
      }
    }
    
    .log-content {
      flex: 1;
      overflow: auto;
      padding: 20px 24px;
      background-color: #f8f8fa;
      border-radius: 0 0 12px 12px;
      font-family: "SF Mono", SFMono-Regular, Consolas, "Liberation Mono", Menlo, monospace;
      
      pre {
        margin: 0;
        color: #333;
        font-size: 0.9rem;
        line-height: 1.5;
      white-space: pre-wrap;
        word-break: break-word;
        
        ::selection {
          background: rgba(0, 113, 227, 0.2);
        }
        
        // 设置不同日志级别的颜色
        :deep(.log-info) {
          color: #1c1c1e;
        }
        
        :deep(.log-warn) {
          color: #9f6000;
        }
        
        :deep(.log-error) {
          color: #d40000;
        }
        
        :deep(.log-debug) {
          color: #6a737d;
        }
        
        :deep(.log-trace) {
          color: #5c2699;
        }
        
        :deep(.log-timestamp) {
          color: #0071e3;
          font-weight: 500;
        }
      }
    }
  }
}

// 确认弹窗内容样式
.fix-confirm-content {
  padding: 20px;
  background-color: rgba(255, 69, 58, 0.05);
  border-radius: 10px;
  border-left: 4px solid @apple-red;
  margin: 0;
  
  p {
    margin: 0 0 10px;
    line-height: 1.6;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}

.loading-container {
      position: relative;
  min-height: 100px;
}

.custom-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: @apple-gray;
      font-size: 14px;
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

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
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
</style>
