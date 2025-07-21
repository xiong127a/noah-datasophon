<!--
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
 -->
<template>
  <div class="parcel-management">
    <a-spin :spinning="spinning">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-content">
          <div class="page-title">集群存储库</div>
          <div class="page-description">管理和配置存储库，查看可用的组件包</div>
        </div>
      </div>

      <!-- 存储库列表 -->
      <div class="repos-container">
        <!-- 内置存储库 -->
        <a-card class="repo-card built-in-repo" title="内置存储库">
          <template #extra>
            <a-tag color="blue">系统默认</a-tag>
          </template>
          <div class="repo-content">
            <div class="repo-info">
              <div class="repo-icon">
                <a-icon type="folder" style="font-size: 24px; color: #3b82f6;" />
              </div>
              <div class="repo-details">
                <div class="repo-path">{{ ddhParcelPath }}</div>
                <div class="repo-status">
                  <a-badge status="success" text="已连接" />
                </div>
              </div>
            </div>
          </div>
        </a-card>

        <!-- 第三方存储库 -->
        <a-card class="repo-card third-party-repos" title="第三方存储库">
          <template #extra>
            <a-button type="primary" @click="addNewRepo" size="small">
              <a-icon type="plus" />添加存储库
            </a-button>
          </template>
          
          <div v-if="parcelList.length === 0" class="empty-state">
            <a-empty description="暂无第三方存储库">
              <a-button type="primary" @click="addNewRepo">添加第一个存储库</a-button>
            </a-empty>
          </div>

          <div v-else>
            <div v-for="parcel in parcelList" :key="parcel.parcelId" class="repo-item">
              <a-card class="parcel-card" size="small">
                <template #title>
                  <div class="repo-title">
                    <a-icon type="cloud" class="repo-type-icon" />
                    {{ parcel.parcelName }}
                  </div>
                </template>
                <template #extra>
                  <a-button type="link" size="small" @click="removeRepo(parcel)" style="color: #ef4444;">
                    <a-icon type="delete" />删除
                  </a-button>
                </template>

                <div class="repo-config">
                  <a-input-search
                    v-model="parcel.parcelPath"
                    placeholder="请输入存储库URL地址"
                    enter-button="解析"
                    @search="onSearch(parcel)"
                    class="repo-url-input"
                  />
                </div>

                <!-- 组件列表 -->
                <div v-if="parcel.components && parcel.components.length > 0" class="components-section">
                  <a-divider orientation="left">可用组件</a-divider>
                  <div class="components-list">
                    <div v-for="comp in parcel.components" :key="comp.name" class="component-item">
                      <div class="component-info">
                        <div class="component-icon">
                          <a-icon type="appstore" style="font-size: 20px; color: #6b7280;" />
                        </div>
                        <div class="component-details">
                          <div class="component-name">{{ comp.label }}</div>
                          <div class="component-version">版本: {{ comp.version }}</div>
                          <div class="component-description">{{ comp.description }}</div>
                        </div>
                        <div class="component-actions">
                          <a-button 
                            v-if="comp.state == undefined" 
                            type="primary" 
                            size="small"
                            @click="handleDownload(comp, parcel.parcelPath)"
                            :loading="comp.state === 'executing' && comp.step === 'download'"
                          >
                            <a-icon type="download" />下载
                          </a-button>
                          <a-button 
                            v-else-if="comp.state === 'success' && comp.step === 'download'" 
                            size="small"
                            @click="handleInstall(comp, parcel.parcelPath)"
                            :loading="comp.state === 'executing' && comp.step === 'install'"
                            style="margin-left: 8px;"
                          >
                            <a-icon type="setting" />安装
                          </a-button>
                          <a-tag v-else-if="comp.state === 'success' && comp.step === 'install'" color="green">
                            <a-icon type="check-circle" />已安装
                          </a-tag>
                        </div>
                      </div>
                      
                      <!-- 进度条 -->
                      <div v-if="comp.state !== undefined" class="component-progress">
                        <a-progress 
                          :percent="comp.process" 
                          :status="comp.state === 'fail' ? 'exception' : comp.state === 'success' ? 'success' : 'active'"
                          :format="percent => formatState(percent, comp)"
                          :stroke-width="6"
                        />
                      </div>
                    </div>
                  </div>
                </div>
            </a-card>
          </div>
        </div>
        </a-card>
      </div>
    </a-spin>
  </div>
</template>
<script>
export default {
    name: "ParcelList",
    data() {
        return {
            loading: false,
            spinning: false,
            ddhParcelPath: "file:///opt/datasophon/DDP/packages",
            formState: {
                name: "XXXXXX"
            },
            parcelList: [],
            taskObj: {},
            parcelProcess: {
                open: false,
                data: "",
                name: "",
                state: undefined,
                taskId: undefined,
                process: 0,
                rolllogThread: undefined
            },
        };
    },
    methods: {
        getParcelList() {
            this.parcelList = [];
            /*
            this.spinning = true;
            this.$axiosPost(global.API.getParcelList, {}).then((res) => {
                this.spinning = false;
                if (res.code === 200) {
                    this.parcelList = res.data;
                }
            });
            */
        },

        addNewRepo() {
            const newRepo = {
                parcelId: Date.now(),
                parcelName: `第三方存储库 ${this.parcelList.length + 1}`,
                parcelPath: "",
                parcelFit: 1,
                frame: "DDP-1.0.0",
                components: []
            };
            this.parcelList.push(newRepo);
        },

        removeRepo(parcel) {
            this.$confirm({
                title: '确认删除',
                content: `确定要删除存储库 "${parcel.parcelName}" 吗？`,
                okText: '确定',
                cancelText: '取消',
                onOk: () => {
                    const index = this.parcelList.findIndex(p => p.parcelId === parcel.parcelId);
                    if (index > -1) {
                        this.parcelList.splice(index, 1);
                        this.$message.success('存储库删除成功');
                    }
                }
            });
        },

        onSearch(parcel){
            if(parcel.parcelPath == "") {
                this.$message.warning('请输入 Parcel 存储库地址。')
                return;
            }
            console.log(parcel);
            this.$axiosPost(global.API.getParcelParse, {url: parcel.parcelPath }).then((res) => {
                if (res.code === 200) {
                    parcel.components = res.data.components;
                }
            });
        },
        formatState(percent, comp) {
            console.log(comp)
            if(this.taskObj && comp.step == 'download') {
                if(comp.state == 'executing') {
                    return "正在下载：" + percent + "%";
                } else if(comp.state == 'success') {
                    return "下载成功";
                } else {
                    return "下载失败";
                }
            } else if (this.taskObj && comp.step == 'install') {
                if (comp.state == 'executing') {
                    return "正在安装：" + percent + "%";
                } else if (comp.state == 'success') {
                    return "安装成功";
                } else {
                    return "安装失败";
                }
            }
            return percent + "%";
        },
        handleDownload(comp, url) {
            if(this.taskObj && this.taskObj.state == 'executing') {
                this.$message.warning('一个操作正在进行, 请稍后操作。')
                return;
            }
            console.log(comp);
            this.$axiosPost(global.API.downloadComponent, { url: url, parcelName: comp.name }).then((res) => {
                if (res.code === 200) {
                    comp.md5 = res.data.md5;
                    comp.process = (res.data.process * 100);
                    comp.state = res.data.state;
                    comp.step = res.data.step;

                    this.parcelProcess.open = true;
                    this.viewTaskLog(comp)
                }
            });
        },
        handleInstall(comp, url) {
            if (this.taskObj && this.taskObj.state == 'executing') {
                this.$message.warning('一个操作正在进行, 请稍后操作。')
                return;
            }
            console.log(comp);
            this.$axiosPost(global.API.installComponent, { md5: comp.md5, packageName: comp.name }).then((res) => {
                if (res.code === 200) {
                    comp.process = (res.data.process * 100);
                    comp.state = res.data.state;
                    comp.step = res.data.step;

                    this.parcelProcess.open = true;
                    this.viewTaskLog(comp)
                }
            });
        },

        viewTaskLog(row) {
            this.taskObj = row
            this.parcelProcess.state = "executing"
            //滚动查看日志
            const _this = this;
            this.$axiosGet(global.API.getParcelProcess, { md5: row.md5}).then(response => {
                if (response.code === 200) {
                    row.state = response.data.state
                    row.process = (response.data.process * 100)

                    if (this.parcelProcess.rolllogThread != undefined) {
                        clearTimeout(this.parcelProcess.rolllogThread);
                        this.parcelProcess.rolllogThread = undefined
                    }

                    if (response.data.process >= 100 && response.data.state != 'executing') {
                        this.parcelProcess.open = false;
                    }

                    // 窗口在打开着，并且进度小于 100, 任务未完成，一直获取
                    if (response.data.process <= 100 && response.data.state == 'executing' && this.parcelProcess.open) {
                        this.parcelProcess.rolllogThread = setTimeout(() => { this.viewTaskLog(row) }, 3000);
                    }
                }
            })
        },
    },
    mounted() {
        this.parcelProcess.open = false;
        this.getParcelList();
    },
};
</script>

<style lang="less" scoped>
.parcel-management {
  padding: 24px;
  background: #fafafa;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;

  .page-header {
    margin-bottom: 24px;
    
    .header-content {
      background: white;
      padding: 24px;
      border-radius: 8px;
      border: 1px solid #e5e7eb;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      
      .page-title {
        margin: 0 0 8px 0;
        font-size: 20px;
        font-weight: 600;
        color: #374151;
      }
      
      .page-description {
        margin: 0;
        font-size: 14px;
        color: #6b7280;
        font-weight: 400;
      }
    }
  }

  .repos-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .repo-card {
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e5e7eb;
    background: white;
    transition: all 0.2s ease;
    
    &:hover {
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      border-color: #d1d5db;
    }
    
    :global(.ant-card-head) {
      border-bottom: 1px solid #e5e7eb;
      padding: 16px 20px;
      
      .ant-card-head-title {
        font-size: 16px;
        font-weight: 600;
        color: #374151;
      }
    }
    
    :global(.ant-card-body) {
      padding: 20px;
    }
  }

  .built-in-repo {
    .repo-content {
      .repo-info {
        display: flex;
        align-items: center;
        
        .repo-icon {
          margin-right: 16px;
        }
        
        .repo-details {
          flex: 1;
          
          .repo-path {
            font-size: 14px;
            font-weight: 500;
            color: #374151;
            margin-bottom: 4px;
          }
          
          .repo-status {
            font-size: 13px;
            
            :global(.ant-badge-status-text) {
              font-weight: 400;
              color: #6b7280;
            }
          }
        }
      }
    }
  }

  .third-party-repos {
    .empty-state {
      text-align: center;
      padding: 40px 20px;
      
      :global(.ant-empty-description) {
        color: #6b7280;
        font-size: 14px;
        font-weight: 400;
      }
    }
    
    .repo-item {
      margin-bottom: 16px;
      
      &:last-child {
        margin-bottom: 0;
      }
    }

    .parcel-card {
      .repo-title {
        display: flex;
        align-items: center;
        
        .repo-type-icon {
          margin-right: 8px;
          color: #3b82f6;
          font-size: 14px;
        }
      }
      
      .repo-config {
        margin-bottom: 16px;
        
        .repo-url-input {
          width: 100%;
        }
      }
        
      .components-section {
        margin-top: 16px;
        
        :global(.ant-divider) {
          margin: 16px 0 12px;
          
          .ant-divider-inner-text {
            font-weight: 600;
            color: #374151;
            font-size: 14px;
          }
        }
        
        .components-list {
          .component-item {
            border: 1px solid #e5e7eb;
            border-radius: 6px;
            margin-bottom: 12px;
            background: white;
            
            &:last-child {
              margin-bottom: 0;
            }
            
            .component-info {
              display: flex;
              align-items: center;
              padding: 16px;
              
              .component-icon {
                margin-right: 12px;
              }
              
              .component-details {
                flex: 1;
                
                .component-name {
                  font-size: 14px;
                  font-weight: 600;
                  color: #374151;
                  margin-bottom: 4px;
                }
                
                .component-version {
                  font-size: 12px;
                  color: #6b7280;
                  margin-bottom: 4px;
                }
                
                .component-description {
                  font-size: 12px;
                  color: #9ca3af;
                  line-height: 1.4;
                }
              }
              
              .component-actions {
                display: flex;
                align-items: center;
                gap: 8px;
              }
            }
            
            .component-progress {
              padding: 0 16px 16px;
              
              :global(.ant-progress) {
                .ant-progress-bg {
                  border-radius: 4px;
                }
                
                .ant-progress-inner {
                  border-radius: 4px;
                  background: #f3f4f6;
                }
              }
            }
          }
        }
      }
    }
  }
}

// 全局样式优化
:global(.ant-btn) {
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.2s ease;
  
  &:not(.ant-btn-link) {
    height: 32px;
    padding: 0 16px;
    font-size: 13px;
  }
  
  &.ant-btn-primary {
    background: #3b82f6;
    border-color: #3b82f6;
    
    &:hover {
      background: #2563eb;
      border-color: #2563eb;
    }
  }
  
  &.ant-btn-sm {
    height: 28px;
    padding: 0 12px;
    font-size: 12px;
  }
}

:global(.ant-tag) {
  border-radius: 4px;
  font-weight: 500;
  border: none;
}

:global(.ant-input) {
  border-radius: 6px;
  height: 32px;
  font-size: 13px;
  
  &:focus {
    border-color: #3b82f6;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
  }
}

:global(.ant-input-search) {
  .ant-input {
    border-radius: 6px 0 0 6px;
  }
  
  .ant-input-search-button {
    border-radius: 0 6px 6px 0;
    background: #3b82f6;
    border-color: #3b82f6;
    
    &:hover {
      background: #2563eb;
      border-color: #2563eb;
    }
  }
}
</style>
