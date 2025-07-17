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
-->
<template>
  <div class="check-items-container">
    <div class="check-items-header">
      <!-- 头部汇总信息 -->
      <div class="header-summary">
        <span class="header-title">共 {{ checkItems.length }} 项检查</span>

        <div class="status-tag success">
          <a-icon type="check-circle" />
          {{ successCount }} 项通过
        </div>

        <div class="status-tag failed">
          <a-icon type="close-circle" />
          {{ failedCount }} 项失败
        </div>

        <div class="status-tag waiting">
          <a-icon type="clock-circle" />
          {{ waitingCount }} 项待检查
        </div>

        <div class="status-tag checking">
          <a-icon type="loading" spin />
          {{ checkingCount }} 项检查中
        </div>

        <div class="status-tag skipped">
          <a-icon type="warning" />
          {{ skippedCount }} 项已跳过
        </div>
      </div>

      <!-- 头部操作按钮 -->
      <div class="header-actions">
        <button
          class="action-button primary"
          :class="{'disabled': !hasRetryableSelectedItems}"
          :disabled="!hasRetryableSelectedItems"
          @click="$emit('retry-selected', record.ip)"
        >
          <a-icon type="redo" />
          重试选中项
        </button>

        <button
          class="action-button secondary"
          :class="{'disabled': !hasFixableSelectedItems}" 
          :disabled="!hasFixableSelectedItems"
          @click="$emit('fix-selected', record.ip)"
        >
          <a-icon type="tool" />
          修复选中项
        </button>
      </div>
    </div>

    <!-- 分隔线 -->
    <div class="divider"></div>

    <!-- 表格内容 -->
    <a-table
      :columns="columns"
      :dataSource="checkItems"
      :pagination="false"
      size="middle"
      rowKey="id"
      :rowSelection="{
        selectedRowKeys: selectedRowKeys,
        onChange: (selectedKeys) => $emit('selected-change', record.ip, selectedKeys)
      }"
      class="apple-style-table"
    ></a-table>
  </div>
</template>

<script>
export default {
  name: 'HostCheckItems',
  props: {
    // 主机记录
    record: {
      type: Object,
      required: true
    },
    // 检查项列表
    checkItems: {
      type: Array,
      default: () => []
    },
    // 选中的检查项ID
    selectedRowKeys: {
      type: Array,
      default: () => []
    },
    // 可以重试的选中项
    hasRetryableSelectedItems: {
      type: Boolean,
      default: false
    },
    // 可以修复的选中项
    hasFixableSelectedItems: {
      type: Boolean,
      default: false
    },
    // 主机是否在检查中
    isHostChecking: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    // 成功的检查项数量
    successCount() {
      return this.checkItems.filter(item => item.status === 'SUCCESS').length;
    },
    // 失败的检查项数量
    failedCount() {
      return this.checkItems.filter(item => item.status === 'FAILED').length;
    },
    // 等待检查的检查项数量
    waitingCount() {
      return this.checkItems.filter(item => item.status === 'WAITING').length;
    },
    // 检查中的检查项数量
    checkingCount() {
      return this.checkItems.filter(item => item.status === 'CHECKING').length;
    },
    // 已跳过的检查项数量
    skippedCount() {
      return this.checkItems.filter(item => item.status === 'SKIPPED').length;
    },
    // 表格列定义
    columns() {
      return [
        {
          title: '检查项',
          dataIndex: 'itemName',
          key: 'itemName',
          width: '30%',
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
          width: '15%',
          customRender: (text, row) => {
            const h = this.$createElement;

            // 检查文本是否存在
            if (!text) return h('span', {}, ['-']);

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
                  text.length > 20 ?
                    this.stripHtml(text).substr(0, 20) + '...' :
                    this.stripHtml(text)
                ])
              ])
            ]);
          }
        },
        {
          title: '操作',
          key: 'action',
          width: '30%',
          customRender: (text, row) => {
            const h = this.$createElement;
            const isChecking = row.status === 'CHECKING';
            const isFailed = row.status === 'FAILED';

            return h('div', {
              class: 'action-buttons',
              style: {
                display: 'flex',
                gap: '8px',
                justifyContent: 'space-between'
              }
            }, [
              // 左侧操作按钮组
              h('div', {
                style: {
                  display: 'flex',
                  gap: '8px'
                }
              }, [
                // 终止/重试按钮 (共享同一位置)
                h('div', {
                  style: {
                    width: '68px',
                    minWidth: '68px',
                    display: 'flex',
                    justifyContent: 'center'
                  }
                }, [
                  // 检查中状态显示终止按钮，否则显示重试按钮
                  isChecking ? 
                  h('button', {
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
                      justifyContent: 'center',
                      whiteSpace: 'nowrap',
                      minWidth: '68px',
                      width: '100%'
                    },
                    on: {
                      click: () => this.$emit('stop-check-item', this.record.ip, row.id)
                    }
                  }, [
                    h('a-icon', {
                      props: { type: 'close' },
                      style: { marginRight: '4px', fontSize: '12px' }
                    }),
                    "终止"
                  ]) : 
                  h('button', {
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
                      justifyContent: 'center',
                      whiteSpace: 'nowrap',
                      minWidth: '68px',
                      width: '100%'
                    },
                    attrs: {
                      disabled: row.status === 'CHECKING' || row.status === 'FIXING' ||
                        !((row.status === 'FAILED' || row.status === 'SUCCESS' || row.status === 'SKIPPED'))
                    },
                    on: {
                      click: () => this.$emit('retry-check-item', this.record.ip, row.id)
                    }
                  }, [
                    h('a-icon', {
                      props: { type: 'redo' },
                      style: { marginRight: '4px', fontSize: '12px' }
                    }),
                    "重试"
                  ])
                ]),

                // 修复按钮占位
                h('div', {
                  style: {
                    width: '68px',
                    minWidth: '68px',
                    display: 'flex',
                    justifyContent: 'center'
                  }
                }, [
                  // 失败状态才显示修复按钮
                  isFailed ? h('button', {
                    style: {
                      border: 'none',
                      backgroundColor: 'rgba(88, 86, 214, 0.1)',
                      color: '#5856D6',
                      padding: '6px 12px',
                      borderRadius: '12px',
                      fontSize: '13px',
                      fontWeight: '500',
                      cursor: this.isHostChecking || row.status === 'FIXING' ? 'not-allowed' : 'pointer',
                      transition: 'all 0.2s ease',
                      opacity: this.isHostChecking || row.status === 'FIXING' ? '0.5' : '1',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      whiteSpace: 'nowrap',
                      minWidth: '68px',
                      width: '100%'
                    },
                    attrs: {
                      disabled: this.isHostChecking || row.status === 'FIXING'
                    },
                    on: {
                      click: () => this.$emit('fix-check-item', this.record.ip, row)
                    }
                  }, [
                    h('a-icon', {
                      props: { type: 'tool' },
                      style: { marginRight: '4px', fontSize: '12px' }
                    }),
                    "修复"
                  ]) : null
                ]),

                // 跳过按钮占位
                h('div', {
                  style: {
                    width: '68px',
                    minWidth: '68px',
                    display: 'flex',
                    justifyContent: 'center'
                  }
                }, [
                  // 失败状态才显示跳过按钮
                  isFailed ? h('button', {
                    style: {
                      border: 'none',
                      backgroundColor: 'rgba(142, 142, 147, 0.1)',
                      color: '#8E8E93',
                      padding: '6px 12px',
                      borderRadius: '12px',
                      fontSize: '13px',
                      fontWeight: '500',
                      cursor: this.isHostChecking ? 'not-allowed' : 'pointer',
                      transition: 'all 0.2s ease',
                      opacity: this.isHostChecking ? '0.5' : '1',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      whiteSpace: 'nowrap',
                      minWidth: '68px',
                      width: '100%'
                    },
                    attrs: {
                      disabled: this.isHostChecking
                    },
                    on: {
                      click: () => this.$emit('skip-check-item', this.record.ip, row.id)
                    }
                  }, [
                    h('a-icon', {
                      props: { type: 'forward' },
                      style: { marginRight: '4px', fontSize: '12px' }
                    }),
                    "跳过"
                  ]) : null
                ])
              ]),

              // 右侧固定位置的查看日志按钮
              h('div', {
                style: {
                  width: '90px',
                  minWidth: '90px',
                  display: 'flex',
                  justifyContent: 'center'
                }
              }, [
                // 查看日志按钮
                h('button', {
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
                    justifyContent: 'center',
                    whiteSpace: 'nowrap',
                    minWidth: '90px',
                    width: '100%'
                  },
                  attrs: {
                    disabled: row.status === 'WAITING'
                  },
                  on: {
                    click: () => this.$emit('view-item-log', this.record.ip, row.id, row.itemName)
                  }
                }, [
                  h('a-icon', {
                    props: { type: 'file-text' },
                    style: { marginRight: '4px', fontSize: '12px' }
                  }),
                  "查看日志"
                ])
              ])
            ]);
          }
        }
      ];
    }
  },
  methods: {
    // 去除HTML标签的辅助函数
    stripHtml(html) {
      if (!html) return '';
      const tmp = document.createElement('DIV');
      tmp.innerHTML = html;
      return tmp.textContent || tmp.innerText || '';
    }
  }
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
@apple-purple: #5856d6;

.check-items-container {
  padding: 24px;
  background-color: @apple-white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin: 0;
  font-family: "SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif;
  
  .check-items-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 16px;
    
    .header-summary {
      font-family: "SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif;
      font-size: 16px;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
      
      .header-title {
        font-weight: 600;
        color: @apple-black;
      }
      
      .status-tag {
        display: flex;
        align-items: center;
        padding: 4px 12px;
        border-radius: 12px;
        font-size: 14px;
        font-weight: 500;
        
        i {
          margin-right: 6px;
        }
        
        &.success {
          background-color: rgba(52, 199, 89, 0.1);
          color: #34c759;
        }
        
        &.failed {
          background-color: rgba(255, 59, 48, 0.1);
          color: #ff3b30;
        }
        
        &.waiting {
          background-color: rgba(255, 149, 0, 0.1);
          color: #ff9500;
        }
        
        &.checking {
          background-color: rgba(0, 122, 255, 0.1);
          color: #007aff;
        }
        
        &.skipped {
          background-color: rgba(142, 142, 147, 0.1);
          color: #8e8e93;
        }
      }
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
      
      .action-button {
        border: none;
        border-radius: 12px;
        padding: 8px 16px;
        font-size: 14px;
        font-weight: 500;
        transition: all 0.2s ease;
        display: flex;
        align-items: center;
        cursor: pointer;
        white-space: nowrap;
        min-width: 120px;
        justify-content: center;
        
        i {
          margin-right: 6px;
        }
        
        &.disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
        
        &.primary {
          background-color: @apple-blue;
          color: white;
          
          &:hover:not(.disabled) {
            background-color: @apple-blue-hover;
          }
        }
        
        &.secondary {
          background-color: @apple-purple;
          color: white;
          
          &:hover:not(.disabled) {
            background-color: darken(@apple-purple, 5%);
          }
        }
      }
    }
  }
  
  .divider {
    height: 1px;
    background-color: rgba(0, 0, 0, 0.05);
    margin: 0 0 20px 0;
  }
  
  .apple-style-table {
    border-radius: 12px;
    overflow: hidden;
  }
}
</style> 