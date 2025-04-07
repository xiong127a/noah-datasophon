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


 * @Date: 2022-06-13 14:04:05
 * @LastEditTime: 2023-04-13 15:13:22
 * @FilePath: \ddh-ui\src\components\steps\stpesRf.vue
-->
<template>
  <div class="steps-rf">
    <div class="steps-rf-container">
      <Steps1 ref="steps1Ref" v-if="stepsNumber === 1" :steps1="steps1Data" />
      <Steps2 ref="steps2Ref" v-if="stepsNumber === 2" :steps1Data="steps1Data" :depType="depType" />
      <Steps3 ref="steps3Ref" v-if="stepsNumber === 3 " />
      <Steps4 ref="steps4Ref" v-if="stepsNumber === 4" :steps4Data="steps4Data" :stepsType="stepsType"
        :depType="depType" />
      <Steps5 ref="steps5Ref" v-if="stepsNumber === 5" :steps4Data="steps4Data" />
      <Steps6 ref="steps6Ref" v-if="stepsNumber === 6" :steps4Data="steps4Data" />
      <Steps7 ref="steps7Ref" v-if="stepsNumber === 7" :steps4Data="steps4Data" />
      <Steps8 ref="steps8Ref" v-if="stepsNumber === 8" :steps4Data="steps4Data" />
    </div>
    <div class="footer">
      <a-button class="mgr10" @click="closeModal">取消</a-button>
      <a-button v-if="stepsNumber > 1 && stepsNumber !== 8" class="mgr10" type="primary" @click="back">上一步</a-button>
      <a-button class="mgr10" type="primary" :loading="nextLoading" @click="next">{{ currentSteps !== stepsList.length ?
        '下一步' : '完成'}}</a-button>
    </div>
  </div>
</template>
<script>
import { mapState, mapMutations, mapActions } from "vuex";

import Steps1 from "./step1.vue";
import Index from "./step2/index.vue";
import Steps3 from "./step3.vue";
import Steps4 from "./step4.vue";
import Steps5 from "./step5.vue";
import Steps6 from "./step6.vue";
import Steps7 from "./step7.vue";
import Steps8 from "./step8.vue";
import { loadRoutes, setDynamicRouter } from "@/utils/routerUtil";

// import cluterRoutes from '@/router/config-cluster'
export default {
  name: "StepsContainer",
  components: {
    Steps1,
    Steps2: Index,
    Steps3,
    Steps4,
    Steps5,
    Steps6,
    Steps7,
    Steps8,
  },
  props: { currentSteps: Number, stepsList: Array, interval: Number, stepsType: String, serviceData: Object, depType:String, },
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId" , 'onSearch'],
  data() {
    return {
      nextLoading: false,
      steps1Data: {
        hosts: "",
        sshUser: "",
        sshPort: "",
        sshPassword: "",
      },
      steps4Data: {
        serviceIds: [],
        serviceNames: [],
      },
    };
  },
  watch: {
    currentSteps(val) {
      console.log(val, "asdsdsa");
    },
    stepsType: {
      handler (val) {
        if (val === 'service-example')  this.steps4Data = {...this.serviceData}
        if (val === 'hostManage')  this.steps1Data = {...this.steps1Data, sshUser: 'root', 'sshPort': '22'} 

      },
      immediate: true
    }
  },
  computed: {
    stepsNumber () {
      if (this.currentSteps === 4 && this.depType == 'K8S'){
        return this.currentSteps + 1
      }//暂时的
      if (this.currentSteps === 5 && this.depType == 'K8S') {
        return this.currentSteps + 1
      }//暂时的
      if (this.currentSteps === 6 && this.depType == 'K8S') {
        return this.currentSteps + 1
      }//暂时的
      if (this.currentSteps === 7 && this.depType == 'K8S') {
      return this.currentSteps + 1
      }//暂时的
      if (this.currentSteps === 3 && this.depType == 'K8S'){
        return this.currentSteps + 1
      }else{
        return this.currentSteps + this.interval
      }
    }
  },
  methods: {
    ...mapActions("steps", ["setClusterId"]),
    ...mapMutations("setting", ["setIsCluster", "setMenuData"]),
    closeModal() {
      this.handleCancel();
    },
    back() {
      this.currentStepsSub();
    },
    async next() {
      // this.nextLoading = true
      let flag = true;
      if (this.stepsNumber === 1) {
        this.$refs.steps1Ref.form.validateFields((err, values) => {
          if (!err) {
            flag = true;
            this.steps1Data = values;
            
            // 直接进入下一步，不再调用clearHostEnvCheckCache
            this.currentStepsAdd();
          } else {
            flag = false;
          }
        });
        return; // 添加return，防止下方的代码执行
      }
      if (this.stepsNumber === 2) {
        const self = this;
        this.$refs.steps2Ref.hostCheckCompleted((res) => {
          this.nextLoading = false;
          flag = res.hostCheckCompleted;
          if (!flag) self.$message.warning(res.data || "存在未检验成功的主机");
          if (!flag) return false;
          
          // 主机检查完成后，直接分析主机列表
          this.$axiosPost(global.API.analysisHostList, {
            clusterId: this.clusterId,
            ips: this.steps1Data.hosts,
            sshUser: this.steps1Data.sshUser,
            sshPort: this.steps1Data.sshPort,
            sshPassword: this.steps1Data.sshPassword,
            page: 1,
            pageSize: 10 // 使用与第三步相同的默认pageSize
          }).then((analysisRes) => {
            if (analysisRes.code !== 200) {
              console.warn("分析主机列表失败:", analysisRes.msg);
              self.$message.warning("分析主机列表失败，请检查主机状态");
            }
            // 无论分析结果如何，都进入下一步
            this.currentStepsAdd();
          }).catch((err) => {
            console.error("分析主机列表出错:", err);
            // 即使分析出错，也进入下一步
            this.currentStepsAdd();
          });
        });
      }
      if (this.stepsNumber === 3) {
        const self = this;
        this.$refs.steps3Ref.dispatcherHostAgentCompleted((res) => {
          this.nextLoading = false;
          flag = res.dispatcherHostAgentCompleted;
          if (!flag) self.$message.warning("存在为未分发完成的主机");
          // if (!flag) return false;
          if (this.stepsList.length === this.currentSteps) {
            this.handleCancel();
            this.onSearch()
          } else {
            this.currentStepsAdd();
          }
        });
      }
      if (this.stepsNumber === 4) {
        //  这个地方过滤掉已经回显的服务 只传递给下一步新选的服务
        this.steps4Data.serviceIds = _.cloneDeep(this.stepsType=='cluster'?this.$refs.steps4Ref.selectedRowKeysArr: this.$refs.steps4Ref.selectedRowKeys);
        this.steps4Data.serviceNames = _.cloneDeep(this.stepsType == 'cluster' ? this.$refs.steps4Ref.selectedRowNamesArr: this.$refs.steps4Ref.selectedRowNames);
        let arr = this.$refs.steps4Ref.dataSource.filter(item => item.installed)
        if (this.depType!=='K8S'){
          arr.map((item, index) => {
            let curIndex = this.steps4Data.serviceIds.indexOf(item.id)
            if (curIndex !== -1) {
              let serviceId = this.steps4Data.serviceIds[curIndex]
              let nameIndex = this.steps4Data.serviceNames.findIndex(nameItem => nameItem.serviceId === serviceId)
              this.steps4Data.serviceIds.splice(curIndex, 1)
              this.steps4Data.serviceNames.splice(nameIndex, 1)
            }
          })
        }
        // && arr.length < 1
        if (this.steps4Data.serviceIds.length < 1) {
          this.$message.warning("请至少选择一个服务");
          flag = false;
        }
        this.steps4Data.serviceIds=[...new Set(this.steps4Data.serviceIds)]
        await this.$axiosPost('/ddh/service/install/checkServiceDependency', {
          clusterId: this.clusterId,
          serviceIds:this.steps4Data.serviceIds.join(',')
        }).then((res) => { 
          flag = res.code == 200
          // flag = res.code == 500//暂时的
          if(res.code != 200)return true
        })
      }
      if (this.stepsNumber === 5) {
        // flag = this.$refs.steps5Ref.handleSubmit();
        this.$refs.steps5Ref.handleSubmit((res) => {
          this.nextLoading = false;
          if (res.code !== 200) return false;
          this.currentStepsAdd();
        });
      }
      if (this.stepsNumber === 6) {
        this.$refs.steps6Ref.handleSubmit((res) => {
          this.nextLoading = false;
          if (res.code !== 200) return false;
          this.currentStepsAdd();
        });
      }
      if (this.stepsNumber === 7) {
        this.$refs.steps7Ref.nextSteps((res) => {
          this.nextLoading = false;
          if (res.code !== 200) return false;
          this.currentStepsAdd();
        });
      }
      if (this.stepsNumber === 8) {
        this.$axiosPost(global.API.getServiceListByCluster, {
          clusterId: this.clusterId,
        }).then((res) => {
          // let menuData = this.$store.state.setting.menuData;
          // menuData.forEach((item) => {
          //   if (item.path === "service-manage") {
          //     item.children = []
          //     res.data.map((serviceItem) => {
          //       item.children.push({
          //         name: serviceItem.serviceName,
          //         meta: {
          //           params: {serviceId: serviceItem.id},
          //           obj: serviceItem,
          //           authority: {
          //             permission: "*",
          //           },
          //           permission: [
          //             {
          //               permission: "*",
          //             },
          //             {
          //               permission: "*",
          //             },
          //           ],
          //         },
          //         fullPath: `/service-manage/service-list/${serviceItem.id}`,
          //         path: `service-list/${serviceItem.id}`,
          //         component: () => import("@/pages/serviceManage/index"),
          //       });
          //     });
          //   }
          // });
          // this.setMenuData(menuData);
          // localStorage.setItem('menuData', JSON.stringify(menuData))
          // localStorage.setItem('isCluster', 'isCluster')
          // this.setIsCluster('isCluster');
          // this.setClusterId(this.clusterId);
          this.handleCancel();
          // this.$router.push("/overview");
        });
        // setDynamicRouter()
      }
      if (![2, 3, 5, 6, 7, 8].includes(this.stepsNumber)) {
        this.nextLoading = false;
        if (!flag) return false;
        this.currentStepsAdd();
      }
    },
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

// 苹果设计系统字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.steps-rf {
  height: 100%;
  display: flex;
  justify-content: space-between;
  flex-direction: column;
  
  // 添加通用的steps类样式
  :deep(.steps) {
    padding-bottom: 30px; // 为所有步骤组件添加底部内边距，给footer留出空间
  }
  
  .steps-rf-container {
    flex: 1;
    overflow-y: auto;
    padding-bottom: 16px; // 减小底部padding，因为footer不再是固定定位
  }
  
  .footer {
    width: 100%;
    height: 80px;
    background: rgba(255, 255, 255, 0.85); // 稍微调整透明度
    backdrop-filter: blur(20px); // 增强模糊效果
    -webkit-backdrop-filter: blur(20px);
    border-top: none; // 移除分割线
    display: flex;
    justify-content: center;
    align-items: center;
    position: relative;
    margin-top: 20px;
    z-index: 10;
    box-shadow: 0 -8px 16px rgba(0, 0, 0, 0.03); // 更微妙的阴影
    
    button {
      height: 44px;
      min-width: 120px; // 确保按钮宽度一致
      padding: 0 24px;
      font-size: 15px;
      font-weight: 500;
      border-radius: 22px;
      transition: all 0.25s cubic-bezier(0.2, 0.1, 0, 1); // 苹果风格的过渡效果
      margin: 0 10px;
      letter-spacing: -0.01em; // 微调字间距
      
      // 取消按钮样式 - 轻色调设计
      &.ant-btn {
        background: transparent; // 透明背景
        border: 1px solid rgba(0, 0, 0, 0.1); // 微妙的边框
        color: @apple-black;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
        
        &:hover {
          background: rgba(0, 0, 0, 0.03);
          border-color: rgba(0, 0, 0, 0.15);
          transform: translateY(-1px);
        }
        
        &:active {
          background: rgba(0, 0, 0, 0.06);
          transform: translateY(0);
          transition: all 0.1s;
        }
      }
      
      // 下一步按钮样式 - 主要操作按钮
      &.ant-btn-primary {
        background: @apple-blue;
        border: none;
        color: white;
        box-shadow: 0 2px 8px rgba(0, 113, 227, 0.2); // 蓝色阴影效果
        
        &:hover {
          background: @apple-blue-hover;
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(0, 113, 227, 0.3);
        }
        
        &:active {
          background: darken(@apple-blue, 5%);
          transform: translateY(0);
          box-shadow: 0 1px 4px rgba(0, 113, 227, 0.2);
          transition: all 0.1s;
        }
        
        &.ant-btn-loading {
          opacity: 0.9;
          pointer-events: none;
          box-shadow: 0 2px 6px rgba(0, 113, 227, 0.15);
          
          .anticon {
            margin-right: 6px;
          }
        }
      }
    }
    
    /deep/ .ant-btn.ant-btn-loading:not(.ant-btn-circle):not(.ant-btn-circle-outline):not(.ant-btn-icon-only) {
      padding-left: 24px;
      
      .anticon {
        margin-right: 6px;
      }
    }
  }
}
</style>