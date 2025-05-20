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


 * @describe: step8-安装并启动服务 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2023-03-15 10:37:53
 * @FilePath: \ddh-ui\src\components\steps\step8.vue
-->
<template>
  <div class="steps8 steps">
    <div class="steps-title flex-bewteen-container pdr30">
      <div>
        <a-icon v-if="currentPage !== 1" type="left" @click="goBack" />
         {{title}}
        </div>
      <!-- <div class="close-x" @click="handleCancel">X</div> -->
      <a-button @click="handleCancel" class="mgb16" style="height: 28px;position: absolute;right: 20px;top:15px;z-index:2" icon="close" />
      <!-- <div v-if="currentPage === 1" class="flex-bewteen-container"> -->
        <!-- <div class="status-num mgr20">
          <span :class="[hostType === 'all' ? 'host-selected' : '']" @click="changeType('all')">
            全部
            <span>10</span>
          </span>
          <a-divider type="vertical" />
          <span :class="[hostType === '1' ? 'host-selected' : '']" @click="changeType('1')">
            安装中
            <span>10</span>
          </span>
          <a-divider type="vertical" />
          <span :class="[hostType === '2' ? 'host-selected' : '']" @click="changeType('2')">
            成功
            <span>10</span>
          </span>
          <a-divider type="vertical" />
          <span :class="[hostType === '3' ? 'host-selected' : '']" @click="changeType('3')">
            失败
            <span>10</span>
          </span>
        </div>-->
       <!-- <a-button type="primary" @click="retryHost('all')">全部重试</a-button>-->
      <!-- </div> -->
    </div>
    <div class="table-info mgt16 steps-body" style="overflow-y: visible;max-height: 700px;">
      <a-table v-if="currentPage === 1" @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" :scroll="{y: 500}" :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" rowKey="commandId" :pagination="pagination"></a-table>
      <a-table v-if="[2,3].includes(currentPage)" @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" :scroll="{y: 500}" rowKey="hostCommandId" :pagination="pagination"></a-table>
      <LOGS v-if="currentPage === 4" :logData="logData" :hideCancel="true" />
    </div>
    <!-- <div class="cluster-setting-footer pdr30" v-if="stepsType === 'cluster-setting'">
      <a-button type="primary" @click="handleCancel">关闭</a-button>
    </div> -->
  </div>
</template>
<script>
import { mapActions, mapState } from "vuex";
import LOGS from "@/components/logs";

export default {
  inject: ["clusterId", "handleCancel"],
  props: {
    stepsType: {
      type: String,
      default: "cluster",
    },
  },
  components: { LOGS },
  data() {
    return {
      hostType: "all",
      title: "安装并启动服务",
      selectedRowKeys: [],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      dataSource: [],
      timer1: null,
      timer2: null,
      timer3: null,
      loading: false,
      currentPage: 1,
      commandId: "", // 第二个列表请求页面需要的参数
      hostname: "", // 第三个列表请求页面需要的参数
      commandHostId: "", // 第三个列表请求页面需要的参数
      commandName: "",
      logData: "",
      // 添加进度追踪记录
      progressHistory: {},
    };
  },
  watch: {
    stepsType: {
      handler(val) {
        if (this.stepsType === "cluster-setting") {
          this.title = "后台操作";
        }
        if (this.stepsType === "service") {
          // this.currentSteps = 4
        }
      },
      immediate: true,
    },
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
      setting: (state) => state.setting
    }),
    columns() {
      let arr = [
        {
          title: "序号",
          key: "index",
          width: 120,
          customRender: (text, row, index) => {
            return (
              <span>
                {parseInt(
                  this.pagination.current === 1
                    ? index + 1
                    : index +
                        1 +
                        this.pagination.pageSize * (this.pagination.current - 1)
                )}
              </span>
            );
          },
        },
        {
          title:
            this.currentPage === 1
              ? "命令"
              : this.currentPage === 2
                ? "主机"
                : "指令名称",
          key: this.currentPage === 2 ? "hostname" : "commandName",
          dataIndex: this.currentPage === 2 ? "hostname" : "commandName",
          width: 300,
          customRender: (text, row, index) => {
            return this.currentPage !== 3 ? (
              <span class={"command-name"} onClick={() => this.seeDetail(row)}>
                {text}
              </span>
            ) : (
              <span>{text}</span>
            );
          },
        },
        {
          title: "状态",
          key: "commandProgress",
          dataIndex: "commandProgress",
          customRender: (text, row, index) => {
            return (
              <span>
                {row.commandStateCode === 1 ? (
                  <a-progress
                    class="progress-warp"
                    percent={text}
                    status="active"
                  />
                ) : row.commandStateCode === 2 ? (
                  <a-progress class="progress-warp" percent={text} />
                ) : row.commandStateCode === 4 ? (
                  <a-progress class="progress-warp" strokeColor='#FFA53D' format={()=><a-icon style="color:#FFA53D" type="exclamation-circle" />} percent={text} />
                ) : (
                  <a-progress
                    class="progress-warp"
                    percent={text}
                    status="exception"
                  />
                )}
              </span>
            );
          },
        },
      ];
      if (this.currentPage === 1) {
        arr.push(
          {
            title: "开始时间",
            key: "createTime",
            dataIndex: "createTime",
            width: 180,
          },
          {
            title: "持续时间",
            key: "durationTime",
            dataIndex: "durationTime",
            width: 160,
          }
        );
      }
      if (this.currentPage === 3) {
        arr.push({
          title: "日志信息",
          key: "resultMsg",
          dataIndex: "resultMsg",
          // width: 140,
          customRender: (text, row, index) => {
            return (
              <span
                class="flex-container command-name"
                onClick={() => this.seeDetail(row)}
              >
                查看日志
              </span>
            );
          },
        });
      }
      return arr;
    },
  },
  methods: {
    changeType(type) {
      this.hostType = type;
    },
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.pollingSearch();
    },
    getServiceList(flag) {
      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      };
      if (this.currentPage === 2) params.commandId = this.commandId;
      if (this.currentPage === 3) {
        params.hostname = this.hostname;
        params.commandHostId = this.commandHostId;
      }
      const ajaxApi =
        this.currentPage === 1
          ? global.API.getServiceCommandlist
          : this.currentPage === 2
            ? global.API.getServiceHostList
            : global.API.getServiceRoleOrderList;
            
      // 记录API请求开始时间，用于计算接口响应时间
      const apiStartTime = new Date().getTime();
      console.log(`[进度调试] 正在请求${this.currentPage}级页面数据，请求API: ${ajaxApi}`, params);
      
      // todo：这个接口地址需要替换
      this.$axiosPost(ajaxApi, params).then((res) => {
        this.loading = false;
        
        // 计算接口响应时间
        const apiResponseTime = new Date().getTime() - apiStartTime;
        console.log(`[进度调试] 接口响应时间: ${apiResponseTime}ms`);
        
        // 记录前后数据变化
        const oldData = this.dataSource && this.dataSource.length > 0 ? 
          this.dataSource.map(item => ({
            id: this.currentPage === 1 ? item.commandId : (this.currentPage === 2 ? item.commandHostId : item.hostCommandId),
            progress: item.commandProgress,
            stateCode: item.commandStateCode
          })) : [];
            
        this.dataSource = res.data;
        this.pagination.total = res.total;
        
        // 比较前后数据变化
        const newData = res.data.map(item => ({
          id: this.currentPage === 1 ? item.commandId : (this.currentPage === 2 ? item.commandHostId : item.hostCommandId),
          name: this.currentPage === 1 ? item.commandName : (this.currentPage === 2 ? item.hostname : item.commandName),
          progress: item.commandProgress,
          state: item.commandState,
          stateCode: item.commandStateCode
        }));
        
        // 记录所有项目的进度历史
        const currentTime = new Date().toLocaleTimeString();
        newData.forEach(item => {
          this.recordProgressHistory(item.id, item.progress, item.stateCode, currentTime);
        });
        
        // 进行进度诊断
        this.diagnoseProgressIssues(newData);
        
        // 进度变化检测
        if (oldData.length > 0) {
          console.log(`[进度调试] 页面${this.currentPage} 数据变化检测:`);
          for (const newItem of newData) {
            const oldItem = oldData.find(item => item.id === newItem.id);
            if (oldItem) {
              const progressDiff = newItem.progress - oldItem.progress;
              const stateChanged = newItem.stateCode !== oldItem.stateCode;
              
              if (progressDiff !== 0 || stateChanged) {
                console.log(`[进度变化] ${newItem.id} (${newItem.name}): 进度 ${oldItem.progress}% -> ${newItem.progress}% (${progressDiff > 0 ? '+' : ''}${progressDiff}%), 状态码: ${oldItem.stateCode} -> ${newItem.stateCode} ${stateChanged ? '(已变化)' : ''}`);
              }
            }
          }
        } else {
          // 首次加载数据，完整记录
          console.log(`[进度调试] 页面${this.currentPage} 首次加载数据:`, newData);
        }
        
        // 添加检查是否有进度异常的数据
        const abnormalItems = newData.filter(item => {
          // 状态是运行中但进度是100%
          return (item.stateCode === 1 && item.progress === 100) || 
                 // 或状态是成功但进度不是100%
                 (item.stateCode === 2 && item.progress !== 100) ||
                 // 或状态是失败但进度不是100%
                 (item.stateCode === 3 && item.progress !== 100);
        });
        
        if (abnormalItems.length > 0) {
          console.warn(`[进度异常] 发现${abnormalItems.length}条数据状态与进度不匹配:`, abnormalItems);
        }
        
        // 添加汇总统计，帮助分析总体进度情况
        if (this.currentPage === 1 && newData.length > 0) {
          const stats = {
            total: newData.length,
            running: newData.filter(item => item.stateCode === 1).length,
            success: newData.filter(item => item.stateCode === 2).length,
            failed: newData.filter(item => item.stateCode === 3).length,
            canceled: newData.filter(item => item.stateCode === 4).length,
            avgProgress: Math.round(newData.reduce((sum, item) => sum + item.progress, 0) / newData.length),
            minProgress: Math.min(...newData.map(item => item.progress)),
            maxProgress: Math.max(...newData.map(item => item.progress))
          };
          console.log(`[进度统计] 命令列表统计: 总数=${stats.total}, 运行中=${stats.running}, 成功=${stats.success}, 失败=${stats.failed}, 取消=${stats.canceled}, 平均进度=${stats.avgProgress}%, 最小进度=${stats.minProgress}%, 最大进度=${stats.maxProgress}%`);
        }
        
        // 如果是主机命令列表，进一步分析子命令的进度计算
        if (this.currentPage === 2 && newData.length > 0) {
          // 找出进度变化异常的主机命令（进度不变但状态变化，或状态不变但进度跳跃）
          if (oldData.length > 0) {
            const suspiciousItems = newData.filter(newItem => {
              const oldItem = oldData.find(item => item.id === newItem.id);
              if (!oldItem) return false;
              
              // 状态变化但进度不变
              const stateChanged = newItem.stateCode !== oldItem.stateCode;
              const progressUnchanged = newItem.progress === oldItem.progress;
              
              // 进度跳跃（一次增加超过20%）
              const progressJump = newItem.progress - oldItem.progress > 20;
              
              return (stateChanged && progressUnchanged) || progressJump;
            });
            
            if (suspiciousItems.length > 0) {
              console.warn(`[进度异常] 发现${suspiciousItems.length}条主机命令进度变化异常:`, suspiciousItems);
            }
          }
        }
      });
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      let arr = row.filter((item) => item.commandStateCode !== 3);
      this.selectedRow = arr;
      this.selectedRowKeys = selectedRowKeys;
    },
    goBack() {
      clearInterval(this.timer1);
      clearInterval(this.timer2);
      clearInterval(this.timer3);
      this.currentPage--;
      this.loading = true;
      if (this.currentPage === 2) {
        this.title = this.commandName;
      }
      if (this.currentPage === 1) {
        this.title = "安装并启动服务";
      }
      if (this.currentPage === 3) {
        this.title = this.hostname;
      }
      this.dataSource = [];
      this.pagination.total = 0;
      this.pagination.current = 1;
      this.pollingSearch();
    },
    seeDetail(row) {
      clearInterval(this.timer1);
      clearInterval(this.timer2);
      clearInterval(this.timer3);
      this.pagination.current = 1;
      if (this.currentPage === 3) {
        this.loading = true;
        this.hostname = row.hostname;
        this.hostCommandId = row.hostCommandId;
        this.getLog();
        return false;
      }
      this.currentPage++;
      this.loading = true;
      if (this.currentPage === 2) {
        this.commandName = row.commandName;
        this.title = row.commandName;
        this.commandId = row.commandId;
        // 添加调试日志，跟踪命令详情
        console.log(`[进度调试] 查看命令详情:`, JSON.stringify({
          commandId: row.commandId,
          commandName: row.commandName,
          progress: row.commandProgress,
          state: row.commandState,
          stateCode: row.commandStateCode
        }));
      }
      if (this.currentPage === 3) {
        this.title = row.hostname;
        this.commandHostId = row.commandHostId;
        this.hostname = row.hostname;
        // 添加调试日志，跟踪主机命令详情
        console.log(`[进度调试] 查看主机命令详情:`, JSON.stringify({
          hostname: row.hostname,
          commandHostId: row.commandHostId,
          progress: row.commandProgress,
          state: row.commandState,
          stateCode: row.commandStateCode
        }));
      }
      this.dataSource = [];
      this.pagination.total = 0;
      this.pollingSearch();
    },
    getLog() {
      this.$axiosPost(global.API.getHostCommandLog, {
        hostCommandId: this.hostCommandId,
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      }).then((res) => {
        this.loading = false
        this.logData = res.data;
        this.currentPage++;
        this.title = "查看日志";
      });
    },
    // 三秒去刷一下
    pollingSearch() {
      this.getServiceList(); // 先立马刷一次
      let self = this;
      if (self[`timer${this.currentPage}`]) {
        clearInterval(self[`timer${this.currentPage}`]);
        console.log(`[进度调试] 清除页面${this.currentPage}的旧轮询定时器`);
      }
      console.log(`[进度调试] 启动页面${this.currentPage}的轮询, 间隔:${global.intervalTime}ms, 时间:${new Date().toLocaleTimeString()}`);
      self[`timer${this.currentPage}`] = setInterval(() => {
        console.log(`[进度调试] 触发轮询刷新 页面${this.currentPage}, 时间:${new Date().toLocaleTimeString()}`);
        self.getServiceList(true);
      }, global.intervalTime);
    },
    // 重试
    retryHost(row) {
      let commandIds = "";
      if (row === "all") {
        if (this.selectedRowKeys.length < 1) {
          this.$message.warning("请至少选择一条命令！");
          return false;
        }
        if (this.selectedRow.length > 0) {
          this.$message.warning("目前只支持失败的命令进行重试操作！");
          return false;
        }
        commandIds = this.selectedRowKeys.join(",");
      } else {
        commandIds = row.commandId;
      }
      const params = {
        commandIds,
        commandType: this.steps.commandType,
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      };
      this.$axiosPost(global.API.startExecuteCommand, params).then((res) => {
        this.selectedRowKeys = [];
        this.$message.success(`操作成功`);
        this.pollingSearch();
      });
    },
    // 取消
    cancelHost(row) {},
    // 主机环境校验是否完成 是否可以进入下一步
    async dispatcherHostAgentCompleted(callback) {
      const params = {
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      };
      // 等待网络请求结束
      let flag = await this.$axiosPost(
        global.API.dispatcherHostAgentCompleted,
        params
      );
      // 网络请求结束后才执行下边的语句  如果传入的callback方法为空或者没传内容也不会去执行，这样也不会影响此方法在别处的调用
      if (callback) {
        callback(flag);
      }
    },
    // 添加一个新方法用于诊断进度条问题
    diagnoseProgressIssues(newData) {
      // 检查是否有进度问题的项目
      const problematicItems = newData.filter(item => {
        // 根据状态和进度的一般规则检查
        if (item.stateCode === 1) { // 运行中
          // 运行中的状态进度不应该是100%
          return item.progress === 100;
        } else if (item.stateCode === 2) { // 成功
          // 成功状态进度必须是100%
          return item.progress !== 100;
        } else if (item.stateCode === 3) { // 失败
          // 失败状态进度通常设置为100%以便UI一致性
          return item.progress !== 100;
        } else if (item.stateCode === 4) { // 取消
          // 取消状态进度通常设置为100%以便UI一致性
          return item.progress !== 100;
        }
        return false;
      });
      
      if (problematicItems.length > 0) {
        console.warn(`[进度诊断] 发现${problematicItems.length}个进度与状态不匹配的项目:`);
        
        problematicItems.forEach(item => {
          let diagnosis = "";
          let recommendation = "";
          
          if (item.stateCode === 1 && item.progress === 100) {
            diagnosis = "运行中状态但进度显示为100%";
            recommendation = "后端应该检查calculateCommandActualProgress方法，确保运行中状态不会将进度设为100%";
          } else if (item.stateCode === 2 && item.progress !== 100) {
            diagnosis = "成功状态但进度不是100%";
            recommendation = "成功状态的进度应始终为100%，检查后端是否正确设置了进度";
          } else if ((item.stateCode === 3 || item.stateCode === 4) && item.progress !== 100) {
            diagnosis = `${item.stateCode === 3 ? '失败' : '取消'}状态但进度不是100%`;
            recommendation = "失败或取消状态的进度通常应设置为100%以保持UI一致性";
          }
          
          // 获取项目的进度历史
          const itemId = item.id;
          const progressHistory = this.getProgressHistory(itemId);
          
          console.warn(`项目ID: ${itemId}, 名称: ${item.name}, 状态码: ${item.stateCode}, 进度: ${item.progress}%`);
          console.warn(`诊断: ${diagnosis}`);
          console.warn(`建议: ${recommendation}`);
          console.warn(`进度历史: ${JSON.stringify(progressHistory)}`);
        });
      }
      
      return problematicItems.length > 0;
    },
    
    // 记录进度历史
    recordProgressHistory(itemId, progress, stateCode, timestamp) {
      if (!this.progressHistory[itemId]) {
        this.progressHistory[itemId] = [];
      }
      
      // 只保留最近10条记录
      if (this.progressHistory[itemId].length >= 10) {
        this.progressHistory[itemId].shift();
      }
      
      this.progressHistory[itemId].push({
        time: timestamp || new Date().toLocaleTimeString(),
        progress,
        stateCode
      });
    },
    
    // 获取进度历史
    getProgressHistory(itemId) {
      return this.progressHistory[itemId] || [];
    },
  },
  mounted() {
    this.pollingSearch();
  },
  beforeDestroy() {
    clearInterval(this.timer1);
    clearInterval(this.timer2);
    clearInterval(this.timer3);
  },
};
</script>
<style lang="less" scoped>
.steps8 {
  .status-num {
    span {
      margin: 0 4px;
      font-size: 14px;
      color: #555555;
      letter-spacing: 0;
      font-weight: 400;
      cursor: pointer;
    }
    span.host-selected {
      color: @primary-color;
      span {
        color: @primary-color;
      }
    }
  }
  .progress-warp {
    width: 80%;
  }
  .command-name {
    color: @primary-color;
    cursor: pointer;
    &:hover {
      text-decoration: underline;
    }
  }
  .cluster-setting-footer {
    display: flex;
    justify-content: flex-end;
  }
  .close-x {
    cursor: pointer;
  }
}
</style>