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
            <!-- 服务卡片列表 -->
            <div class="services-grid-container">
              <div class="services-grid">
                <div 
                  v-for="service in currentFrameServices" 
                  :key="service.id" 
                  class="service-card"
              >
                  <div class="service-card-content">
                    <div class="service-header">
                                              <svg-icon 
                        :icon-class="getServiceIconClass(service.serviceName)" 
                      class="service-icon-img"
                      />
                      <div class="service-title">
                        <div class="service-name">{{ service.serviceName }}</div>
                        <div class="service-version">{{ service.serviceVersion }}</div>
                      </div>
                    </div>
                    
                    <div class="service-description">
                      {{ service.serviceDesc || '暂无描述' }}
                  </div>
                    
                    <div class="service-footer">
                      <a-button 
                        type="danger" 
                        ghost
                        class="delete-btn"
                        @click.stop="onDelete(service)"
                      >
                        <a-icon type="delete" />
                        <span>删除服务</span>
                      </a-button>
                    </div>
                  </div>
                </div>
              </div>
                
              <!-- 空状态 -->
              <div v-if="currentFrameServices.length === 0" class="empty-services">
                <a-empty description="该框架下暂无服务组件" />
              </div>
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
import SvgIcon from '@/icons/SvgIcon.vue';

export default {
  name: "FrameList",
  components: {
    SvgIcon
  },
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

    // 获取服务对应的图标类名
    getServiceIconClass(serviceName) {
      // 将服务名称转为小写
      const iconName = serviceName.toLowerCase();
      
      // 检查是否为已知服务
      const knownServices = [
        'hdfs', 'yarn', 'hbase', 'hive', 'spark', 'flink', 'kafka', 'zookeeper',
        'hadoop', 'hue', 'kylin', 'livy', 'phoenix', 'presto', 'ranger', 
        'solr', 'sqoop', 'tez', 'trino', 'elasticsearch', 'kibana', 'alluxio',
        'atlas', 'airflow', 'flume', 'oozie', 'sentry'
      ];
      
      // 如果是已知服务，返回对应的图标名
      if (knownServices.includes(iconName)) {
        return iconName;
      }
      
      // 对于未知服务，返回默认图标
      return 'service-default';
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
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', Arial, sans-serif;

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
    border-radius: 16px;
    border: 1px solid rgba(0, 0, 0, 0.08);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
    overflow: hidden;
    
    /deep/ .ant-tabs-bar {
      margin: 0;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      background: #f8fafc;
      padding: 0 8px;
      
      .ant-tabs-nav {
        .ant-tabs-tab {
          position: relative;
          margin: 0 8px;
          padding: 16px 16px;
          font-weight: 600;
          font-size: 15px;
          color: #6b7280;
          border: none;
          background: transparent;
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
          
          &::after {
            content: '';
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            height: 3px;
            background: transparent;
            border-radius: 3px 3px 0 0;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
          }
          
          &:hover {
            color: #007AFF;
            
            &::after {
              background: rgba(0, 122, 255, 0.3);
            }
          }
          
          &.ant-tabs-tab-active {
            color: #007AFF;
            background: transparent;
            border-bottom: none;
            
            &::after {
              background: #007AFF;
            }
          }
        }
      }
    }
    
    /deep/ .ant-tabs-content {
      padding: 0;
      background: #fff;
      
      .ant-tabs-tabpane {
        padding: 0;
      }
    }
    
    .services-grid-container {
      padding: 24px;
      
      .services-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 24px;
        
        .service-card {
          background: white;
          border-radius: 16px;
          overflow: hidden;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
          border: 1px solid rgba(0, 0, 0, 0.05);
          height: 100%;
          
          &:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
          }
          
          .service-card-content {
            padding: 24px;
            display: flex;
            flex-direction: column;
            height: 100%;
            justify-content: space-between;
            
            .service-header {
              display: flex;
              align-items: center;
              margin-bottom: 16px;
              
              .service-icon-img {
                color: #007AFF;
                font-size: 28px;
                margin-right: 16px;
                flex-shrink: 0;
              }
              
              .service-title {
                flex: 1;
                overflow: hidden;
                
                .service-name {
                  font-size: 18px;
                  font-weight: 600;
                  color: #1f2937;
                  margin-bottom: 4px;
                  white-space: nowrap;
                  overflow: hidden;
                  text-overflow: ellipsis;
                }
                
                .service-version {
                  font-size: 13px;
                  color: #6b7280;
                  padding: 2px 10px;
                  background: #f3f4f6;
                  border-radius: 20px;
                  display: inline-block;
          }
        }
      }
      
            .service-description {
              font-size: 14px;
              color: #4b5563;
              line-height: 1.6;
              margin: 8px 0;
              min-height: 60px;
              overflow: hidden;
              text-overflow: ellipsis;
              display: -webkit-box;
              -webkit-line-clamp: 2;
              -webkit-box-orient: vertical;
            }
            
                          .service-footer {
        display: flex;
                justify-content: flex-end;
        align-items: center;
                margin-top: auto;
                padding-top: 16px;
                
                .delete-btn {
                  font-size: 13px;
                  color: #ff4d4f;
                  border-color: #ff4d4f;
                  border-radius: 6px;
                  padding: 0 16px;
          height: 32px;
                  
                  /deep/ span {
                    display: inline-block !important; /* 强制显示按钮文本 */
                  }
                  
                  &:hover {
                    color: #ffffff !important;
                    background-color: #ff4d4f !important;
                    border-color: #ff4d4f !important;
                    opacity: 1;
                  }
                  
                  &:active {
                    background-color: #cf1322 !important;
                    border-color: #cf1322 !important;
                  }
                  
                  .anticon {
                    margin-right: 4px;
                  }
                }
            }
          }
        }
        }
        
      .empty-services {
        padding: 40px 0;
        text-align: center;
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
