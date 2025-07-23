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
            <div class="system-default-tag">系统默认</div>
          </template>
          <div class="repo-content">
            <div class="repo-info">
              <div class="repo-icon">
                <i class="system-folder-icon"></i>
              </div>
              <div class="repo-details">
                <div class="repo-path">{{ ddhParcelPath }}</div>
                <div class="repo-status">
                  <span class="status-dot success"></span>
                  <span class="status-text">已连接</span>
                </div>
              </div>
            </div>
          </div>
        </a-card>

        <!-- 第三方存储库 -->
        <a-card class="repo-card third-party-repos" title="第三方存储库">
          <template #extra>
            <a-button type="primary" @click="addNewRepo" size="small" class="apple-button add-button">
              <i class="button-icon plus-icon"></i>添加存储库
            </a-button>
          </template>
          
          <div v-if="parcelList.length === 0" class="empty-state">
            <a-empty description="暂无第三方存储库">
              <a-button type="primary" @click="addNewRepo" class="apple-button">添加第一个存储库</a-button>
            </a-empty>
          </div>

          <div v-else>
            <div v-for="parcel in parcelList" :key="parcel.parcelId" class="repo-item">
              <a-card class="parcel-card" size="small">
                <template #title>
                  <div class="repo-title">
                    <i class="cloud-icon"></i>
                    {{ parcel.parcelName }}
                  </div>
                </template>
                <template #extra>
                  <a-button type="link" size="small" @click="removeRepo(parcel)" class="delete-button">
                    <i class="trash-icon"></i>删除
                  </a-button>
                </template>

                <div class="repo-config">
                  <a-input-search
                    v-model="parcel.parcelPath"
                    placeholder="请输入存储库URL地址"
                    enter-button="解析"
                    @search="onSearch(parcel)"
                    class="repo-url-input apple-input-search"
                  />
                </div>

                <!-- 组件列表 -->
                <div v-if="parcel.components && parcel.components.length > 0" class="components-section">
                  <div class="section-title">可用组件</div>
                  <div class="components-list">
                    <div v-for="comp in parcel.components" :key="comp.name" class="component-item">
                      <div class="component-info">
                        <div class="component-icon">
                          <i class="app-icon"></i>
                        </div>
                        <div class="component-details">
                          <div class="component-name">{{ comp.label }}</div>
                          <div class="component-meta">
                            <span class="component-version">版本: {{ comp.version }}</span>
                          </div>
                          <div class="component-description">{{ comp.description || '暂无描述' }}</div>
                        </div>
                        <div class="component-actions">
                          <a-button 
                            v-if="comp.state == undefined" 
                            type="primary" 
                            size="small"
                            @click="handleDownload(comp, parcel.parcelPath)"
                            :loading="comp.state === 'executing' && comp.step === 'download'"
                            class="apple-button download-button"
                          >
                            <i class="download-icon"></i>下载
                          </a-button>
                          <a-button 
                            v-else-if="comp.state === 'success' && comp.step === 'download'" 
                            size="small"
                            @click="handleInstall(comp, parcel.parcelPath)"
                            :loading="comp.state === 'executing' && comp.step === 'install'"
                            class="apple-button install-button"
                          >
                            <i class="install-icon"></i>安装
                          </a-button>
                          <div v-else-if="comp.state === 'success' && comp.step === 'install'" class="installed-tag">
                            <i class="check-icon"></i>
                            <span>已安装</span>
                          </div>
                        </div>
                      </div>
                      
                      <!-- 进度条 -->
                      <div v-if="comp.state !== undefined" class="component-progress">
                        <a-progress 
                          :percent="comp.process" 
                          :status="comp.state === 'fail' ? 'exception' : comp.state === 'success' ? 'success' : 'active'"
                          :format="percent => formatState(percent, comp)"
                          :stroke-width="4"
                          class="apple-progress"
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
        injectGlobalStyle() {
            // 创建全局样式元素
            const styleEl = document.createElement('style');
            styleEl.id = 'apple-confirm-modal-style';
            styleEl.innerHTML = `
                .apple-style-modal .ant-modal-content {
                    border-radius: 12px !important;
                    background-color: rgba(255, 255, 255, 0.98) !important;
                    backdrop-filter: blur(10px) !important;
                }
                
                .apple-style-modal .ant-modal-confirm-body-wrapper {
                    padding: 24px !important;
                }
                
                .apple-style-modal .ant-modal-confirm-title {
                    color: #1d1d1f !important;
                    font-weight: 600 !important;
                    font-size: 18px !important;
                    text-align: center !important;
                    padding-left: 0 !important;
                }
                
                .apple-style-modal .ant-modal-confirm-content {
                    margin-top: 16px !important;
                    margin-left: 0 !important;
                    text-align: center !important;
                    font-size: 14px !important;
                    color: #6e6e73 !important;
                }
                
                .apple-style-modal .ant-modal-confirm-btns {
                    float: none !important;
                    text-align: center !important;
                    margin-top: 24px !important;
                }
                
                .apple-style-modal .ant-btn {
                    min-width: 80px !important;
                    height: 40px !important;
                    border-radius: 8px !important;
                    font-size: 15px !important;
                    font-weight: 500 !important;
                    border: none !important;
                    padding: 0 16px !important;
                    margin: 0 8px !important;
                }
                
                .apple-style-modal .ant-btn-default {
                    background-color: rgba(0, 0, 0, 0.05) !important;
                    color: #1d1d1f !important;
                }
                
                .apple-style-modal .ant-btn-default:hover {
                    background-color: rgba(0, 0, 0, 0.1) !important;
                }
                
                .apple-style-modal .ant-btn-primary {
                    background-color: #FF453A !important;
                    color: white !important;
                }
                
                .apple-style-modal .ant-btn-primary:hover {
                    background-color: #ee281d !important;
                }
                
                .apple-style-modal .anticon {
                    display: none !important;
                }
                
                .apple-style-modal .ant-modal-confirm-body {
                    text-align: center !important;
                }
            `;
            document.head.appendChild(styleEl);
        },
        
        removeGlobalStyle() {
            const styleEl = document.getElementById('apple-confirm-modal-style');
            if (styleEl) {
                styleEl.parentNode.removeChild(styleEl);
            }
        },

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

        appleStyleConfirm(title, content, onOk) {
            // 直接使用简单的$confirm API
            this.$confirm({
                title: '确认删除',
                content: `确定要删除存储库 "${content}" 吗？`,
                okText: '确定',
                cancelText: '取消',
                icon: null,
                okType: 'danger',
                width: 340,
                centered: true,
                wrapClassName: 'apple-style-modal',
                onOk
            });
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
            this.appleStyleConfirm(
                '确认删除', 
                parcel.parcelName,
                () => {
                    const index = this.parcelList.findIndex(p => p.parcelId === parcel.parcelId);
                    if (index > -1) {
                        this.parcelList.splice(index, 1);
                        this.$message.success('存储库删除成功');
                    }
                }
            );
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
        
        // 添加全局样式，确保弹窗按钮正确显示
        this.injectGlobalStyle();
    },
    
    beforeDestroy() {
        // 移除全局样式，避免影响其他页面
        this.removeGlobalStyle();
    },
};
</script>

<style lang="less" scoped>
.parcel-management {
  padding: 24px;
  background: #f5f5f7;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif;

  .page-header {
    margin-bottom: 24px;
    
    .header-content {
      background: rgba(255, 255, 255, 0.85);
      padding: 28px 24px;
      border-radius: 12px;
      border: none;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      backdrop-filter: blur(10px);
      
      .page-title {
        margin: 0 0 8px 0;
        font-size: 22px;
        font-weight: 600;
        color: #1d1d1f;
        letter-spacing: -0.01em;
      }
      
      .page-description {
        margin: 0;
        font-size: 14px;
        color: #6e6e73;
        font-weight: 400;
        letter-spacing: -0.01em;
      }
    }
  }

  .repos-container {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .repo-card {
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    border: none;
    background: rgba(255, 255, 255, 0.85);
    transition: all 0.3s ease;
    backdrop-filter: blur(10px);
    overflow: hidden;
    
    &:hover {
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
    }
    
    /deep/ .ant-card-head {
      min-height: 56px;
      border-bottom: 1px solid rgba(60, 60, 67, 0.1);
      padding: 0 20px;
      
      .ant-card-head-title {
        font-size: 16px;
        font-weight: 600;
        color: #1d1d1f;
        letter-spacing: -0.01em;
        padding: 16px 0;
      }
      
      .ant-card-extra {
        padding: 14px 0;
      }
    }
    
    /deep/ .ant-card-body {
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
          
          .system-folder-icon {
            display: inline-block;
            width: 32px;
            height: 32px;
            background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%230A84FF' d='M19.5 21a3 3 0 003-3V6a3 3 0 00-3-3h-7.8a1.2 1.2 0 01-.856-.352L9.156 1.056A3.6 3.6 0 006.6 0H4.5a3 3 0 00-3 3v15a3 3 0 003 3h15zM3 6V3a1.5 1.5 0 011.5-1.5h2.1a2.1 2.1 0 011.5.614l1.688 1.592A2.7 2.7 0 0011.7 4.5H19.5A1.5 1.5 0 0121 6v1H3V6zm0 3h18v9a1.5 1.5 0 01-1.5 1.5h-15A1.5 1.5 0 013 18V9z'/%3E%3C/svg%3E");
            background-size: contain;
            background-repeat: no-repeat;
            background-position: center;
          }
        }
        
        .repo-details {
          flex: 1;
          
          .repo-path {
            font-size: 14px;
            font-weight: 500;
            color: #1d1d1f;
            margin-bottom: 6px;
          }
          
          .repo-status {
            font-size: 13px;
            display: flex;
            align-items: center;
            
            .status-dot {
              width: 8px;
              height: 8px;
              border-radius: 50%;
              margin-right: 6px;
              
              &.success {
                background-color: #30D158;
                box-shadow: 0 0 6px rgba(48, 209, 88, 0.5);
              }
            }
            
            .status-text {
              color: #6e6e73;
              font-weight: 400;
            }
          }
        }
      }
    }
  }

  .third-party-repos {
    .empty-state {
      text-align: center;
      padding: 48px 20px;
      
      /deep/ .ant-empty-description {
        color: #6e6e73;
        font-size: 14px;
        font-weight: 400;
        margin-bottom: 16px;
      }
    }
    
    .repo-item {
      margin-bottom: 16px;
      
      &:last-child {
        margin-bottom: 0;
      }
    }

    .parcel-card {
      /deep/ .ant-card-head {
        background: rgba(0, 0, 0, 0.02);
      }
      
      .repo-title {
        display: flex;
        align-items: center;
        
        .cloud-icon {
          display: inline-block;
          width: 18px;
          height: 18px;
          margin-right: 8px;
          background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%230A84FF' d='M6.352 17.79A7.79 7.79 0 1116.846 9.2h.944a5.79 5.79 0 010 11.58H6.352zm0-2h11.438a3.79 3.79 0 000-7.58h-1.78l-.176-.854a5.79 5.79 0 10-9.482 5.944l.338.49h-.338z'/%3E%3C/svg%3E");
          background-size: contain;
          background-repeat: no-repeat;
          background-position: center;
        }
      }
      
      .repo-config {
        margin-bottom: 20px;
        
        .repo-url-input {
          width: 100%;
        }
      }
        
      .components-section {
        margin-top: 20px;
        
        .section-title {
          font-size: 15px;
          font-weight: 600;
          color: #1d1d1f;
          margin-bottom: 16px;
          position: relative;
          padding-left: 14px;
          
          &:before {
            content: '';
            position: absolute;
            left: 0;
            top: 2px;
            bottom: 2px;
            width: 4px;
            background: #0A84FF;
            border-radius: 2px;
          }
        }
        
        .components-list {
          .component-item {
            border-radius: 10px;
            margin-bottom: 16px;
            background: rgba(0, 0, 0, 0.02);
            overflow: hidden;
            transition: all 0.2s ease;
            
            &:hover {
              background: rgba(0, 0, 0, 0.03);
            }
            
            &:last-child {
              margin-bottom: 0;
            }
            
            .component-info {
              display: flex;
              align-items: flex-start;
              padding: 16px;
              
              .component-icon {
                margin-right: 12px;
                
                .app-icon {
                  display: inline-block;
                  width: 24px;
                  height: 24px;
                  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%238E8E93' d='M10 13H4a1 1 0 01-1-1V4a1 1 0 011-1h6a1 1 0 011 1v8a1 1 0 01-1 1zm10 0h-6a1 1 0 01-1-1V4a1 1 0 011-1h6a1 1 0 011 1v8a1 1 0 01-1 1zM10 21H4a1 1 0 01-1-1v-4a1 1 0 011-1h6a1 1 0 011 1v4a1 1 0 01-1 1zm10 0h-6a1 1 0 01-1-1v-4a1 1 0 011-1h6a1 1 0 011 1v4a1 1 0 01-1 1z'/%3E%3C/svg%3E");
                  background-size: contain;
                  background-repeat: no-repeat;
                  background-position: center;
                }
              }
              
              .component-details {
                flex: 1;
                padding-right: 16px;
                
                .component-name {
                  font-size: 15px;
                  font-weight: 600;
                  color: #1d1d1f;
                  margin-bottom: 6px;
                }
                
                .component-meta {
                  display: flex;
                  align-items: center;
                  margin-bottom: 6px;
                  
                  .component-version {
                    font-size: 12px;
                    background: rgba(0, 0, 0, 0.05);
                    padding: 2px 8px;
                    border-radius: 4px;
                    color: #6e6e73;
                  }
                }
                
                .component-description {
                  font-size: 13px;
                  color: #8E8E93;
                  line-height: 1.5;
                }
              }
              
              .component-actions {
                display: flex;
                align-items: center;
                gap: 8px;
                
                .installed-tag {
                  display: flex;
                  align-items: center;
                  background: rgba(48, 209, 88, 0.1);
                  border-radius: 6px;
                  padding: 4px 10px;
                  color: #30D158;
                  font-size: 13px;
                  font-weight: 500;
                  
                  .check-icon {
                    display: inline-block;
                    width: 16px;
                    height: 16px;
                    margin-right: 4px;
                    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%2330D158' d='M12 22C6.477 22 2 17.523 2 12S6.477 2 12 2s10 4.477 10 10-4.477 10-10 10zm-.997-6l7.07-7.071-1.414-1.414-5.656 5.657-2.829-2.829-1.414 1.414L11.003 16z'/%3E%3C/svg%3E");
                    background-size: contain;
                    background-repeat: no-repeat;
                    background-position: center;
                  }
                }
              }
            }
            
            .component-progress {
              padding: 0 16px 16px;
              
              /deep/ .ant-progress {
                &.apple-progress {
                  .ant-progress-bg {
                    border-radius: 2px;
                    background: #0A84FF;
                  }
                  
                  .ant-progress-inner {
                    border-radius: 2px;
                    background: rgba(0, 0, 0, 0.05);
                  }
                  
                  .ant-progress-success-bg {
                    background: #30D158;
                  }
                  
                  .ant-progress-status-exception .ant-progress-bg {
                    background: #FF453A;
                  }
                  
                  .ant-progress-text {
                    color: #6e6e73;
                    font-size: 12px;
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

// 系统默认标签
.system-default-tag {
  display: inline-block;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background-color: rgba(10, 132, 255, 0.1);
  color: #0A84FF;
  font-weight: 500;
}

// Apple 风格按钮
.apple-button {
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
  letter-spacing: -0.01em;
  
  &.ant-btn-primary {
    background: #0A84FF;
    border-color: transparent;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    
    &:hover, &:focus {
      background: #0071E3;
      border-color: transparent;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
    }
    
    &:active {
      background: #0058c4;
      border-color: transparent;
    }
  }
  
  &.ant-btn-sm {
    height: 28px;
    padding: 0 10px;
    font-size: 12px;
  }
  
  .button-icon {
    margin-right: 4px;
    display: inline-block;
    width: 16px;
    height: 16px;
    background-size: contain;
    background-repeat: no-repeat;
    background-position: center;
  }
  
  .plus-icon {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='white' d='M12 5v14M5 12h14'/%3E%3C/svg%3E");
  }
  
  &.add-button {
    background: #0A84FF;
    
    &:hover {
      background: #0071E3;
    }
  }
  
  &.download-button {
    background: #0A84FF;
    
    .download-icon {
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='white' d='M13 10h5l-6 6-6-6h5V3h2v7zm-9 9h16v-7h2v8a1 1 0 01-1 1H3a1 1 0 01-1-1v-8h2v7z'/%3E%3C/svg%3E");
    }
    
    &:hover {
      background: #0071E3;
    }
  }
  
  &.install-button {
    background: #30D158;
    
    .install-icon {
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='white' d='M3.34 17a10 10 0 0114.32-14.32c1.67 1.67 2.68 3.7 3.1 5.82l.3-.02c1.34 0 2.55.53 3.43 1.4a4.8 4.8 0 011.16 4.96l-.13.34c.18.31.29.67.29 1.06a2 2 0 01-2 2h-1.8v-.005H9a3 3 0 01-3-3c0-1.11.6-2.08 1.5-2.6l-.5-1.21A4.97 4.97 0 013.34 17zM7 13.5a5 5 0 110-10 5 5 0 110 10zm0-2a3 3 0 100-6 3 3 0 000 6zm10 2a5 5 0 110-10 5 5 0 110 10zm0-2a3 3 0 100-6 3 3 0 000 6z'/%3E%3C/svg%3E");
    }
    
    &:hover {
      background: #25b04a;
    }
  }
}

// 删除按钮样式
.delete-button {
  display: inline-flex;
  align-items: center;
  color: white;
  border: none;
  background: #FF453A;
  padding: 4px 12px;
  transition: all 0.2s ease;
  border-radius: 6px;
  height: 28px;
  
  .trash-icon {
    display: inline-block;
    width: 14px;
    height: 14px;
    margin-right: 4px;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='white' d='M17 6h5v2h-2v13a1 1 0 01-1 1H5a1 1 0 01-1-1V8H2V6h5V3a1 1 0 011-1h8a1 1 0 011 1v3zm1 2H6v12h12V8zM9 4v2h6V4H9z'/%3E%3C/svg%3E");
    background-size: contain;
    background-repeat: no-repeat;
    background-position: center;
  }
  
  &:hover {
    color: white;
    background: #ee281d;
    box-shadow: 0 2px 4px rgba(255, 69, 58, 0.3);
  }
  
  &:focus {
    color: white;
    background: #ee281d;
  }
}

// Apple 风格输入框
.apple-input-search {
  /deep/ .ant-input {
    border-radius: 8px 0 0 8px;
    height: 36px;
    font-size: 14px;
    border-color: rgba(0, 0, 0, 0.1);
    transition: all 0.2s ease;
    
    &:hover, &:focus {
      border-color: #0A84FF;
      box-shadow: 0 0 0 2px rgba(10, 132, 255, 0.2);
    }
  }
  
  /deep/ .ant-input-group-addon {
    .ant-input-search-button {
      height: 36px;
      border-radius: 0 8px 8px 0;
      background: #0A84FF;
      border-color: #0A84FF;
      box-shadow: none;
      
      &:hover {
        background: #0071E3;
        border-color: #0071E3;
      }
      
      .anticon {
        color: white;
      }
    }
  }
}

// 确认弹窗样式
:global(.apple-confirm-modal) {
  .ant-modal-content {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12);
  }
  
  .ant-modal-body {
    padding: 24px;
  }
  
  .ant-modal-confirm-body-wrapper {
    padding: 0;
  }
  
  .ant-modal-confirm-btns {
    display: flex;
    margin-top: 24px;
    
    .apple-confirm-button {
      flex: 1;
      height: 40px;
      border-radius: 8px;
      font-size: 15px;
      font-weight: 500;
      border: none;
      
      &.cancel-button {
        background: rgba(0, 0, 0, 0.05);
        color: #1d1d1f;
        
        &:hover {
          background: rgba(0, 0, 0, 0.1);
        }
      }
      
      &.delete-confirm-button {
        background: #FF453A;
        color: white;
        margin-left: 12px;
        
        &:hover {
          background: #ee281d;
        }
      }
    }
  }
  
  .ant-modal-confirm-title {
    font-size: 18px;
    font-weight: 600;
    color: #1d1d1f;
    text-align: center;
    margin-bottom: 12px;
  }
  
  .ant-modal-confirm-content {
    font-size: 14px;
    color: #6e6e73;
    text-align: center;
    margin: 0 auto;
    max-width: 400px;
  }
  
  .custom-warning-icon {
    display: none;
  }
  
  .anticon-question-circle {
    display: none;
  }
}

// 自定义动画
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(10, 132, 255, 0.5);
  }
  70% {
    box-shadow: 0 0 0 8px rgba(10, 132, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(10, 132, 255, 0);
  }
}

// 全局弹窗样式 - 使用深度选择器确保样式能够被应用
/deep/ .apple-style-modal {
  .ant-modal-content {
    border-radius: 12px !important;
    overflow: visible !important;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12) !important;
    background-color: rgba(255, 255, 255, 0.98) !important;
    backdrop-filter: blur(10px) !important;
    
    .ant-modal-confirm-body-wrapper {
      padding: 24px !important;
      
      .ant-modal-confirm-title {
        color: #1d1d1f !important;
        font-weight: 600 !important;
        font-size: 18px !important;
        text-align: center !important;
        margin-bottom: 16px !important;
      }
      
      .ant-modal-confirm-content {
        text-align: center !important;
        font-size: 14px !important;
        color: #6e6e73 !important;
        margin-left: 0 !important;
        margin-bottom: 24px !important;
      }
      
      .ant-modal-confirm-btns {
        display: flex !important;
        justify-content: space-between !important;
        float: none !important;
        
        button {
          flex: 1 !important;
          height: 40px !important;
          border-radius: 8px !important;
          font-weight: 500 !important;
          font-size: 15px !important;
          border: none !important;
          
          &:first-child {
            margin-right: 8px !important;
            background-color: rgba(0, 0, 0, 0.05) !important;
            color: #1d1d1f !important;
            
            &:hover {
              background-color: rgba(0, 0, 0, 0.1) !important;
            }
          }
          
          &.ant-btn-primary {
            margin-left: 8px !important;
            
            &.ant-btn-danger {
              background-color: #FF453A !important;
              
              &:hover {
                background-color: #ee281d !important;
              }
            }
          }
        }
      }
    }
  }
  
  .anticon {
    display: none !important;
  }
}
</style>
