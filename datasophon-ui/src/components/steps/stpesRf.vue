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
import Steps2 from "./step2.vue";
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
    Steps2,
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
      if (this.currentSteps === 4 && this.depType === 'Kubernetes'){
        return this.currentSteps + 1
      }//暂时的
      if (this.currentSteps === 5 && this.depType === 'Kubernetes') {
        return this.currentSteps + 1
      }//暂时的
      if (this.currentSteps === 6 && this.depType === 'Kubernetes') {
        return this.currentSteps + 1
      }//暂时的
      if (this.currentSteps === 7 && this.depType === 'Kubernetes') {
      return this.currentSteps + 1
      }//暂时的
      if (this.currentSteps === 3 && this.depType === 'Kubernetes'){
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
        try {
          const values = await new Promise((resolve, reject) => {
            this.$refs.steps1Ref.form.validateFields((err, values) => {
              if (!err) {
                resolve(values);
              } else {
                reject(err);
              }
            });
          });
          
          // 如果是K8S集群，需要先保存K8S配置
          if (this.$refs.steps1Ref.isK8sCluster) {
            const saveResult = await this.$refs.steps1Ref.saveKubernetesConfig();
            if (!saveResult) {
              flag = false;
              return;
            }
            
            // K8S模式下，为steps1Data提供默认值，因为不需要用户输入主机信息
            this.steps1Data = {
              hosts: '', // K8S模式下会自动从集群获取，这里设为空字符串
              sshUser: 'root', // 默认用户
              sshPort: 22, // 默认端口
              kubeConfigContent: values.kubeConfigContent,
              namespace: values.namespace
            };
          } else {
            // PVM模式使用用户输入的值
            this.steps1Data = values;
          }
          
          flag = true;
        } catch (error) {
          flag = false;
        }
      }
      if (this.stepsNumber === 2) {
        const self = this;
        this.$refs.steps2Ref.hostCheckCompleted((res) => {
          this.nextLoading = false;
          flag = res.hostCheckCompleted;
          if (!flag) self.$message.warning("存在为未检验成功的主机");
          if (!flag) return false;
          this.currentStepsAdd();
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
        this.steps4Data.serviceType = this.$refs.steps4Ref.params.type || '';
        
        let arr = this.$refs.steps4Ref.dataSource.filter(item => item.installed)
        if (this.depType!=='Kubernetes'){
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
        
        // 确保数据不为空
        this.steps4Data.serviceIds = [...new Set(this.steps4Data.serviceIds)];
        
        if (this.steps4Data.serviceIds.length < 1) {
          this.$message.warning("请至少选择一个服务");
          flag = false;
          return;
        }
        
        // 检查服务依赖关系
        if (flag) {
          try {
            const res = await this.$axiosPost('/ddh/service/install/checkServiceDependency', {
              clusterId: this.clusterId,
              serviceIds: this.steps4Data.serviceIds.join(',')
            });
            
            if (res.code !== 200) {
              this.$message.error(res.msg || '服务依赖检查失败');
              flag = false;
              return;
            }
          } catch (error) {
            this.$message.error('服务依赖检查异常: ' + (error.message || '未知错误'));
            flag = false;
            return;
          }
        }
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
.steps-rf {
  height: 100%;
  width: 100%;
  display: flex;
  justify-content: space-between;
  flex-direction: column;
  box-sizing: border-box;
  
  .steps-rf-container {
    width: 100%;
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0; /* 确保容器可以缩小 */
    box-sizing: border-box;
    padding: 0 20px; /* 添加一些内边距，避免内容太靠边 */
  }
  
  .footer {
    // margin: 0 32px 0 auto;
    // margin: 0 32px 0 0;
    // margin: 0 auto;
    width: 100%;
    height: 64px;
    background: rgba(242, 244, 247, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    box-sizing: border-box;
    padding: 0 20px; /* 与内容区域保持一致的内边距 */
    button {
      width: 86px;
    }
    /deep/
      .ant-btn.ant-btn-loading:not(.ant-btn-circle):not(.ant-btn-circle-outline):not(.ant-btn-icon-only) {
      padding-left: 20px;
    }
  }
  
  /* 添加媒体查询以处理不同的屏幕尺寸 */
  @media screen and (max-width: 1300px) {
    .footer {
      padding: 0 16px;
    }
  }
  
  /* 确保所有步骤组件都能正确适应容器 */
  /deep/ .steps {
    width: 100%;
    box-sizing: border-box;
    
    /* 在小屏幕上保持一致的布局 */
    @media screen and (max-width: 992px) {
      min-width: max-content; /* 确保内容可以完全显示 */
    }
  }
}
</style>