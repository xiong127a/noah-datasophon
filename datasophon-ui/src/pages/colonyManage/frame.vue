<!--
/*
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


 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-07-27 15:54:37
 * @FilePath: \ddh-ui\src\pages\colonyManage\frame.vue
-->
<template>
  <div class="frame-management">
    <a-spin :spinning="spinning">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-content">
          <div class="page-title">集群框架</div>
          <div class="page-description">查看集群中可安装的服务框架和组件</div>
        </div>
      </div>

      <!-- 框架Tab切换 -->
      <div v-if="frameList.length > 0" class="frames-content">
        <a-tabs v-model="activeFrameCode" @change="onFrameChange">
          <a-tab-pane 
            v-for="frame in frameList" 
            :key="frame.frameCode"
            :tab="frame.frameCode"
          >
            <!-- 服务表格 -->
            <div class="services-table-container">
              <a-table 
                :columns="serviceColumns"
                :data-source="currentFrameServices"
                :pagination="false"
                :scroll="{ x: 800 }"
                row-key="id"
              >
                <template slot="serviceIcon" slot-scope="text, record">
                  <div class="service-icon-cell">
                    <img 
                      :src="getServiceIconPath(record.serviceName)"
                      :alt="record.serviceName"
                      class="service-icon-img"
                      @error="handleIconError"
                    />
                  </div>
                </template>
                
                <template slot="serviceName" slot-scope="text, record">
                  <div class="service-name-cell">
                    <div class="service-name">{{ record.serviceName }}</div>
                    <div class="service-description">{{ record.serviceDesc || '暂无描述' }}</div>
                  </div>
                </template>
                
                <template slot="action" slot-scope="text, record">
                  <a-button 
                    type="link" 
                    size="small" 
                    danger
                    @click="onDelete(record)"
                  >
                    删除
                  </a-button>
                </template>
              </a-table>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>

      <!-- 空状态 -->
      <div v-if="frameList.length === 0" class="empty-frames">
        <a-empty description="暂无集群框架" />
      </div>
    </a-spin>
  </div>
</template>

<script>
export default {
  name: "FrameList",
  data() {
    return {
      loading: false,
      spinning: false,
      frameList: [],
      activeFrameCode: null, // 当前激活的框架代码
      serviceColumns: [
        {
          title: "图标",
          dataIndex: "serviceName",
          key: "serviceIcon",
          width: 80,
          align: "center",
          scopedSlots: { customRender: "serviceIcon" }
        },
        {
          title: "服务名称",
          dataIndex: "serviceName",
          key: "serviceName",
          scopedSlots: { customRender: "serviceName" }
        },
        {
          title: "版本",
          dataIndex: "serviceVersion",
          key: "serviceVersion",
          width: 120
        },
        {
          title: "操作",
          key: "action",
          width: 100,
          align: "center",
          scopedSlots: { customRender: "action" }
        }
      ]
    };
  },
  computed: {
    currentFrameServices() {
      if (!this.activeFrameCode) return [];
      const currentFrame = this.frameList.find(frame => frame.frameCode === this.activeFrameCode);
      return currentFrame ? currentFrame.frameServiceList || [] : [];
    }
  },
  methods: {
    onFrameChange(frameCode) {
      this.activeFrameCode = frameCode;
    },

    getServiceIconPath(serviceName) {
      // 将服务名称转为小写作为SVG图标名称
      const iconName = serviceName.toLowerCase();
      return require(`@/icons/common/${iconName}.svg`);
    },

    handleIconError(event) {
      // 图标加载失败时使用默认图标
      event.target.src = require('@/icons/common/service-default.svg');
    },
    loadTable() {
      let that = this;
      let columns = that.tableColumns;
      return columns.map((item, index) => {
        return {
          title: item.title,
          key: item.key,
          fixed: item.fixed ? item.fixed : "",
          width: item.width ? item.width : "",
          align: item.align ? item.align : "left",
          ellipsis: item.ellipsis ? item.ellipsis : "",
          customRender: (text, record, index) => {
            if (item.key == "index") {
              return `${index + 1}`;
            } else if (item.key == "action") {
              let _this = this
              const child = _this.$createElement('a', {
                domProps: {
                  innerHTML: "删除"
                },
                on: {
                  click: function () {
                    _this.onDelete(record)
                  }
                }
              })
              return child;
            } else {
              return <span title={record[item.key]}> {record[item.key]} </span>;
            }
          },
        };
      });
    },
    getFrameList() {
      this.spinning = true;
      this.$axiosPost(global.API.getFrameList, {}).then((res) => {
        this.spinning = false;
        if (res.code === 200) {
          this.frameList = res.data;
          // 自动选择第一个框架
          if (this.frameList.length > 0 && !this.activeFrameCode) {
            this.activeFrameCode = this.frameList[0].frameCode;
          }
        }
      });
    },
    onDelete(record) {
      console.log(record)
      let self = this
      this.$confirm({
        title: '确认提示',
        okText: '确认',
        cancelText: '取消',
        content:  (
          <div style="margin-top:20px">
            <div style="font-size: 16px;color: #555555;">
              {'是否确认删除 ' + record.serviceName + ' 服务？'}
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => {
                  self.$axiosGet(global.API.deleteService + "/" + record.id, {}).then((res) => {
                    if (res.code === 200) {
                      self.getFrameList();
                      self.$destroyAll();
                    }
                  });
                }}
              >
                确定
              </a-button>
              <a-button
                style="margin-right:10px;"
                onClick={() => self.$destroyAll() }
              >
                取消
              </a-button>
            </div>
          </div>
        ),
        okType: 'danger',
        closable: true,
      });
    }
  },
  mounted() {
    this.getFrameList();
  },
};
</script>

<style lang="less" scoped>
.frame-management {
  padding: 20px;
  background: #fafafa;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;

  .page-header {
    background: #ffffff;
    padding: 32px;
    border-radius: 12px;
    margin-bottom: 24px;
    border: 1px solid #e5e7eb;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    
    .header-content {
      .page-title {
        font-size: 28px;
        font-weight: 600;
        color: #1f2937;
        margin: 0 0 8px 0;
        letter-spacing: -0.025em;
      }

      .page-description {
        color: #6b7280;
        margin: 0;
        font-size: 15px;
        line-height: 1.5;
      }
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
      
      .ant-btn {
        border: none;
        border-radius: 12px;
        height: 44px;
        padding: 0 20px;
        font-weight: 600;
        font-size: 15px;
        transition: all 0.2s ease;
        
        &:not(.ant-btn-primary) {
          background: rgba(255, 255, 255, 0.15);
          color: white;
          backdrop-filter: blur(10px);
          
          &:hover {
            background: rgba(255, 255, 255, 0.25);
            transform: translateY(-1px);
          }
        }
        
        &.ant-btn-primary {
          background: rgba(255, 255, 255, 0.9);
          color: #007AFF;
          
          &:hover {
            background: white;
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
          }
        }
      }
    }
  }

  .frames-content {
    background: #ffffff;
    border-radius: 12px;
    border: 1px solid #e5e7eb;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    overflow: hidden;
    
    :global(.ant-tabs-bar) {
      margin: 0;
      border-bottom: 1px solid #e5e7eb;
      background: #f8fafc;
      
      .ant-tabs-nav {
        .ant-tabs-tab {
          padding: 16px 24px;
          font-weight: 600;
          color: #6b7280;
          border: none;
          background: transparent;
          
          &:hover {
            color: #3b82f6;
          }
          
          &.ant-tabs-tab-active {
            color: #3b82f6;
            background: #ffffff;
            border-bottom: 2px solid #3b82f6;
          }
        }
      }
    }
    
    :global(.ant-tabs-content) {
      padding: 0;
      
      .ant-tabs-tabpane {
        padding: 0;
      }
    }
    
    .services-table-container {
      :global(.ant-table) {
        .ant-table-thead > tr > th {
          background: #f8fafc;
          border-bottom: 1px solid #e5e7eb;
          color: #374151;
          font-weight: 600;
          padding: 16px;
        }
        
        .ant-table-tbody > tr {
          &:hover {
            background: #f8fafc;
          }
          
          > td {
            padding: 16px;
            border-bottom: 1px solid #f3f4f6;
          }
        }
      }
      
      .service-icon-cell {
        display: flex;
        justify-content: center;
        align-items: center;
        
        .service-icon-img {
          width: 32px;
          height: 32px;
          object-fit: contain;
        }
      }
      
      .service-name-cell {
        .service-name {
          font-size: 14px;
          font-weight: 600;
          color: #1f2937;
          margin-bottom: 4px;
        }
        
        .service-description {
          font-size: 12px;
          color: #6b7280;
          line-height: 1.4;
        }
       }
     }
  }
  
  .empty-frames {
    text-align: center;
    padding: 60px 20px;
    
    :global(.ant-empty-description) {
      color: #6b7280;
    }
  }
}

// 危险操作样式
:global(.danger-item) {
  color: #ff3b30 !important;
  
  &:hover {
    background: #fff2f0 !important;
  }
}

// 全局按钮样式优化
:global(.ant-btn) {
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.2s ease;
  
  &:not(.ant-btn-link) {
    height: 40px;
    padding: 0 20px;
  }
  
  &.ant-btn-primary {
    background: #007AFF;
    border-color: #007AFF;
    
    &:hover {
      background: #0056CC;
      border-color: #0056CC;
      transform: translateY(-1px);
    }
  }
  
  &.ant-btn-sm {
    height: 32px;
    padding: 0 16px;
    font-size: 13px;
  }
}

// 下拉菜单样式优化
:global(.ant-dropdown-menu) {
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(0, 0, 0, 0.06);
  padding: 8px;
  
  .ant-dropdown-menu-item {
    border-radius: 8px;
    margin: 2px 0;
    padding: 8px 12px;
    font-weight: 500;
    
    &:hover {
      background: #f6f6f6;
    }
  }
}

// 标签样式优化
:global(.ant-tag) {
  border-radius: 8px;
  font-weight: 600;
  border: none;
}

// 徽章样式优化
:global(.ant-badge-status-text) {
  font-weight: 500;
  color: #86868b;
}

// 响应式设计
@media (max-width: 768px) {
  .frame-management {
    padding: 16px;
    
    .page-header {
      flex-direction: column;
      align-items: stretch;
      gap: 20px;
      padding: 32px 24px;
      border-radius: 16px;
      
      .header-content .page-title {
        font-size: 28px;
        
        .title-icon {
          font-size: 32px;
        }
      }
      
      .header-actions {
        justify-content: center;
        
        .ant-btn {
          height: 40px;
          font-size: 14px;
        }
      }
    }
    
    .frames-overview {
      margin-bottom: 24px;
      
      .frame-overview-card .frame-card-content {
        padding: 20px;
        
        .frame-icon {
          font-size: 32px;
        }
        
        .frame-info .frame-name {
          font-size: 18px;
        }
      }
    }
    
    .frame-details .frame-details-card {
      border-radius: 16px;
      
      :global(.ant-card-head) {
        padding: 20px 24px;
        
        .ant-card-head-title {
          font-size: 20px;
        }
      }
      
      :global(.ant-card-body) {
        padding: 24px;
      }
      
      .services-grid {
        grid-template-columns: 1fr;
        gap: 16px;
      }
    }
  }
}
</style>
