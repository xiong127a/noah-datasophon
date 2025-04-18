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


 * @describe: 服务配置
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-10-27 11:01:48
 * @FilePath: \ddh-ui\src\pages\serviceManage\setting.vue
-->
<template>
  <div class="service-setting steps">
    <div class="flex-bewteen-container" style="flex-direction:row-reverse;">
      <div class="w180" style="margin-right:23px;">
        版本：
        <a-select placeholder="请选择" :value="currentVersion" @change="changeVersion" style="width:180px">
          <a-select-option v-for="(child, childIndex) in verSionList" :key="childIndex" :value="child">{{ child }}
          </a-select-option>
        </a-select>
      </div>
    </div>
    
    <!-- 添加Tab页签 -->
    <a-tabs :activeKey="activeTabKey" @change="handleTabChange">
      <a-tab-pane key="service-config" tab="服务配置" :forceRender="true">
        <div class="flex-bewteen-container" style="align-items: baseline; margin-top:10px;">
          <a-spin :spinning="false" class=" w180  setting" style="display: grid;height:300px;">
            <!-- <a-radio-group :default-value="currentId"  @change="changeCasting" style="margin-left:1px;" >
              <a-radio-button :value="item.id" v-for="(item, childIndex) in GroupList" :key="childIndex" :style="radioStyle" >
               {{item.roleGroupName}}
               </a-radio-button>
            </a-radio-group> -->
            <div v-for="(item, childIndex) in GroupList" :key="childIndex" @click="handlerClick(item,childIndex)"
                 :class="[currentId==item.id ? 'active':'','system']">
              <div :class="[currentId==item.id ? 'active':'','system']">
                {{ item.roleGroupName }}
                <!-- <a-icon  type="sync" class="menu-sub-icon" @click="textCompare" /> -->
                <a-popover trigger="hover" placement="rightTop" class="popover-index" overlayClassName="popover-index"
                           :content="()=> getMoreMenu(item)">
                  <a-icon type="more" class="fr"/>
                </a-popover>
              </div>
            </div>
          </a-spin>
          <a-spin :spinning="loading" class="steps-body" style="position: relative; flex:1; margin:0 20px">
            <!-- 调整模板中的配置组部分 -->
            <div v-for="(group, groupName) in templateData"
                 :key="groupName"
                 class="config-group">
              <!-- 配置组标题 -->
              <h3 class="group-title" @click="toggleGroup(groupName)">
                {{ groupName }}
                <span class="arrow" :class="{ 'arrow-up': isGroupExpanded[groupName] }">▶</span>
              </h3>

              <!-- 配置内容（默认收起） -->
              <div v-show="isGroupExpanded[groupName]">
                <CommonTemplate
                    :ref="`template_${groupName}`"
                    :steps4Data="steps4Data"
                    :templateData="group"
                />
              </div>
            </div>
            <div class="footer">
              <a-button class="mgr10" type="primary" @click="handleSubmit">保存</a-button>
            </div>
          </a-spin>

        </div>
      </a-tab-pane>
      
      <a-tab-pane key="k8s-config" tab="K8s配置" :forceRender="true">
        <K8sConfig :serviceId="serviceId" :serviceName="serviceName" :clusterId="clusterId"/>
      </a-tab-pane>
      
      <a-tab-pane key="config-download" tab="配置文件下载" :forceRender="true">
        <ConfigDownload :serviceId="serviceId" :serviceName="serviceName" />
      </a-tab-pane>
    </a-tabs>

  </div>
</template>
<script>
import CommonTemplate from "@/components/commonTemplate/index";
import {mapActions, mapState} from "vuex";
import RenameGroup from "./renameGroup.vue";
import {getServiceName} from "@/utils/util";
import ConfigDownload from "./config/components/ConfigDownload.vue";
import K8sConfig from "./config/components/K8sConfig.vue";

export default {
  components: {CommonTemplate, ConfigDownload, K8sConfig},
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      loading: false,
      templateData: {},
      verSionList: [],
      GroupList: [],
      currentId: undefined,
      currentVersion: undefined,
      isGroupExpanded: {}, // 存储每个配置组的展开状态
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      labelCol: {
        xs: {span: 24},
        sm: {span: 5},
      },
      wrapperCol: {
        xs: {span: 24},
        sm: {span: 19},
      },
      radioStyle: {
        display: 'block',
        height: '30px',
        lineHeight: '30px',
        marginTop: '5px',
      },
      value: 0,
      activeTabKey: 'service-config',
      serviceId: '',
      serviceName: ''
    };
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
  },
  methods: {
    ...mapActions("steps", ["setCommandType", "setCommandIds"]),
    getMoreMenu(props) {
      let arr = [
        {name: "重命名", key: "rename"},
        {name: "删除", key: "del"},
      ];
      // if (props.meta.obj.needRestart) arr.splice(2, 0, { name: "重启", key: "restart" })
      return arr.map((item, index) => {
        return (
            <div key={index}>
              <a
                  class="more-menu-btn"
                  style="border-width:0px;min-width:100px;color: #333;"
                  onClick={() => this.batchOpt(item, props)}
              >
                {item.name}
              </a>
            </div>
        );
      });
    },
    toggleGroup(groupName) {
      this.$set(this.isGroupExpanded, groupName, !this.isGroupExpanded[groupName])
    },
    changeName(params) {
      this.GroupList.forEach(item => {
        if (item.id === params.roleGroupId) {
          item.roleGroupName = params.roleGroupName
        }
      })
    },
    renameCharacter(props) {
      const self = this;
      let width = 520;
      let title = "重命名";
      let content = (
          <RenameGroup grouopObj={props} callBack={(params) => self.changeName(params)}/>
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div/>;
        },
      });
    },
    batchOpt(item, props) {
      if (item.key === 'rename') {
        this.renameCharacter(props)
        return false
      }
      this.$confirm({
        width: 450,
        title: () => {
          return (
              <div style="font-size: 22px;">
                <a-icon
                    type="question-circle"
                    style="color:#2F7FD1 !important;margin-right:10px"
                />
                提示
              </div>
          );
        },
        content: (
            <div style="margin-top:20px">
              <div style="padding:0 65px;font-size: 16px;color: #555555;">
                {'确认删除吗？'}
              </div>
              <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
                <a-button
                    style="margin-right:10px;"
                    type="primary"
                    onClick={() => this.confirmDel(item, props)}
                >
                  确定
                </a-button>
                <a-button
                    style="margin-right:10px;"
                    onClick={() => this.$destroyAll()}
                >
                  取消
                </a-button>
              </div>
            </div>
        ),
        icon: () => {
          return <div/>;
        },
        closable: true,
      });
    },
    confirmDel(item, props) {
      let serviceName = getServiceName(this.$route.params.serviceId);

      let params = {
        roleGroupId: props.id,
        serviceName: serviceName
      };
      this.$axiosPost(global.API.delGroup, params).then((res) => {
        this.$destroyAll();
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.GroupList = this.GroupList.filter(item => item.id !== props.id)
          if (this.GroupList.length > 0 && this.currentId === props.id) {
            this.currentId = this.GroupList[0].id;
          }
        }
      });
    },
    handlerClick(item, childIndex) {
      console.log(item);
      this.currentId = item.id
      this.getConfigVersion()
    },
    handlearrayWithData(a) {
      let obj = {};
      let arr = [];
      for (let k in a) {
        if (k.includes("arrayWith")) {
          let key = "";
          if (k.includes("arrayWithKey")) {
            key = k.split("arrayWithKey")[0];
            arr.push(key);
          }
          if (k.includes("arrayWithVal")) {
            key = k.split("arrayWithVal")[0];
            arr.push(key);
          }
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      for (let f in obj) {
        let keys = [];
        let vals = [];
        for (let i in a) {
          if (i.includes(f)) {
            if (i.includes("arrayWithKey")) {
              keys.push(i);
            }
            if (i.includes("arrayWithVal")) {
              vals.push(i);
            }
          }
        }
        keys.map((item, index) => {
          obj[f].push({
            [`${a[item]}`]: a[vals[index]],
          });
        });
      }
      return obj;
    },
    handleMultipleData(a) {
      let obj = {};
      let arr = [];
      for (let k in a) {
        if (k.includes("multiple")) {
          let key = k.split("multiple")[0];
          arr.push(key);
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      // obj{ a: , b: }
      for (let f in obj) {
        let vals = [];
        for (let i in a) {
          if (i.includes(f)) {
            if (i.includes("multiple")) {
              vals.push(i);
            }
          }
        }
        vals.map((item, index) => {
          obj[f].push(a[vals[index]]);
        });
      }
      return obj;
    },
    // 单个标签页的保存
    async handleSubmit() {
      try {
        // 1. 获取所有配置组表单实例
        const formRefs = Object.keys(this.templateData)
            .map(groupName => this.$refs[`template_${groupName}`]?.[0])
            .filter(Boolean);

        // 2. 并行验证所有表单
        await Promise.all(
            formRefs.map(form =>
                new Promise((resolve, reject) => {
                  form.form.validateFields()
                      .then(values => resolve(values))
                      .catch(error => reject(error))
                })
            )
        );

        // 3. 合并所有配置数据
        const mergedValues = formRefs.reduce((acc, form) => {
          return { ...acc, ...form.form.getFieldsValue() };
        }, {});

        // 4. 转换数据结构（修复核心问题）
        const param = _.cloneDeep(this.templateData);
        const allConfigItems = Object.values(param).flat(); // 转换为扁平化数组

        // 5. 更新配置项值
        Object.entries(mergedValues).forEach(([name, value]) => {
          const targetItem = allConfigItems.find(item => item.name === name);
          if (targetItem) {
            targetItem.value = value;
          }
        });

        // 6. 处理配置项名称
        const processedItems = allConfigItems.map(item => ({
          ...item,
          name: item.name.replaceAll("!", ".")
        }));

        // 7. 过滤不需要的配置项
        const filterParam = processedItems.filter(
            item => !(!item.required && item.hidden)
        );

        // 8. 获取服务名称（保持原有逻辑）
        let serviceName = '';
        const serviceId = this.$route.params.serviceId || '';
        const menuData = JSON.parse(localStorage.getItem('menuData')) || [];
        const arr = menuData.filter(item => item.path === 'service-manage');
        if (arr.length > 0) {
          arr[0].children.forEach(item => {
            if (item.meta.params.serviceId == serviceId) serviceName = item.name;
          });
        }

        // 9. 构建保存参数
        const saveParam = {
          clusterId: this.clusterId,
          serviceName,
          serviceConfig: JSON.stringify(filterParam),
          roleGroupId: this.currentId
        };

        // 10. 提交保存
        const res = await this.$axiosPost(global.API.saveServiceConfig, saveParam);
        if (res.code === 200) {
          this.$message.success("保存成功");
          this.getConfigVersion();
        } else {
          this.$message.error(res.msg || "保存失败");
        }

      } catch (error) {
        if (error.errorFields) {
          const firstError = error.errorFields[0];
          const fieldPath = firstError.name.join('.');
          this.$message.error(`配置验证失败：[${fieldPath}] ${firstError.errors[0]}`);
        } else {
          this.$message.error(`保存失败: ${error.message || '未知错误'}`);
        }
      }
    },
    changeVersion(val) {
      this.currentVersion = val;
      this.getServiceConfigOption();
    },
    changeCasting(val) {
      console.log(val.target.value);
      this.currentId = val.target.value
      this.getConfigVersion()
    },

    //获取角色组
    getServiceRoleType() {
      this.loading = true;
      const params = {
        serviceInstanceId: this.$route.params.serviceId,
      }
      this.$axiosPost(global.API.getRoleGroupList, params).then((res) => {
        if (res.code !== 200) return  //this.$message.error('获取角色组列表失败')
        this.GroupList = res.data
        if (this.GroupList.length > 0) {
          this.currentId = this.GroupList[0].id;
          //this.getServiceConfigOption( true);
        }
        this.getConfigVersion()
      })
    },
    // 获取服务版本
    getConfigVersion() {
      this.loading = true;
      const params = {
        serviceInstanceId: this.$route.params.serviceId,
        roleGroupId: JSON.stringify(this.currentId) || '',
      };
      this.$axiosPost(global.API.getConfigVersion, params).then((res) => {
        if (res.code === 200) {
          this.verSionList = res.data;
          if (this.verSionList.length > 0) {
            this.currentVersion = this.verSionList[0];
            this.getServiceConfigOption(true);
          }
        }
      });
    },
    // 在获取配置的方法中增加初始化逻辑
    async getServiceConfigOption(loading) {
      if (!loading) this.loading = true;
      const self = this;
      const params = {
        serviceInstanceId: this.$route.params.serviceId,
        page: 1,
        pageSize: 10000,
        "version":this.currentVersion||'',
        "roleGroupId": JSON.stringify(this.currentId)||'',
      };
      const res = await this.$axiosPost(global.API.getConfigInfo, params);

      if (res.code === 200) {
        this.templateData = this.handlerTemplate(res.data);

        // 初始化所有分组为收起状态
        Object.keys(this.templateData).forEach(name => {
          if (!(name in this.isGroupExpanded)) {
            this.$set(this.isGroupExpanded, name, false)
          }
        });
      }
      this.loading = false;
    },
    handlerTemplate(data) {
      const result = {};

      Object.entries(data).forEach(([originalKey, configList]) => {
        // 直接使用原始键名，并进行标准化处理
        const groupKey = originalKey
                ?.trim() // 去除前后空格
                .replace(/^"|"$/g, '') // 去除可能存在的引号
            || 'CommonConfig'; // 空值处理

        // 配置项名称转换（保留原始替换逻辑）
        const processedItems = configList.map(item => ({
          ...item,
          name: (item.name || '').replaceAll(".", "!")
        }));

        // 合并到结果集
        result[groupKey] = [
          ...(result[groupKey] || []),
          ...processedItems
        ];
      });


      // 保证至少存在通用配置组
      if (!('CommonConfig' in result)) {
        result.CommonConfig = [];
      }

      return result;
    },
    handleTabChange(key) {
      this.activeTabKey = key;
      
      // 确保serviceName有值
      if (!this.serviceName && this.$route.params.serviceId) {
        // 从菜单数据中获取服务名称
        const serviceId = this.$route.params.serviceId;
        const menuData = JSON.parse(localStorage.getItem('menuData')) || [];
        const arr = menuData.filter(item => item.path === 'service-manage');
        if (arr.length > 0) {
          arr[0].children.forEach(item => {
            if (item.meta.params.serviceId == serviceId) {
              this.serviceName = item.name;
            }
          });
        }
        
        // 如果还是没有找到，则使用默认值
        if (!this.serviceName) {
          this.serviceName = "未知服务";
          console.warn('无法获取服务名称，使用默认值');
        }
      }
    },
  },
  created() {
    // 从query和params中获取参数
    const queryParams = this.$route.query;
    const routeParams = this.$route.params;
    
    console.log('setting.vue创建, 查询参数:', queryParams);
    console.log('路由参数:', routeParams);
    
    // 优先使用query中的参数，如果没有则使用params中的
    const serviceInstanceId = queryParams.serviceInstanceId || routeParams.serviceId;
    const serviceName = queryParams.serviceName;
    const serviceType = queryParams.serviceType;
    
    console.log('合并后serviceInstanceId:', serviceInstanceId);
    console.log('serviceName:', serviceName);
    console.log('serviceType:', serviceType);
    
    this.serviceId = serviceInstanceId;
    
    // 设置serviceName
    if (serviceName) {
      this.serviceName = serviceName;
    } else if (serviceType) {
      this.serviceName = getServiceName(serviceType);
    } else {
      // 从菜单数据中获取服务名称
      const menuData = JSON.parse(localStorage.getItem('menuData')) || [];
      const arr = menuData.filter(item => item.path === 'service-manage');
      if (arr.length > 0 && arr[0].children) {
        arr[0].children.forEach(item => {
          if (item.meta && item.meta.params && item.meta.params.serviceId == serviceInstanceId) {
            this.serviceName = item.name;
          }
        });
      }
      
      // 如果还是没有找到，使用默认值
      if (!this.serviceName) {
        this.serviceName = "未知服务";
        console.warn('无法获取服务名称，使用默认值');
      }
    }
    
    console.log('设置后的serviceId:', this.serviceId);
    console.log('设置后的serviceName:', this.serviceName);
    
    this.getServiceRoleType();
  },
  mounted() {
    this.getServiceRoleType()
    // setTimeout(()=>{
    //   this.getConfigVersion()
    // },1000)
  },
};
</script>
<style lang="less" scoped>

.service-setting {
  /deep/ .ant-spin-container {
    position: relative;
  }
  .setting{
    overflow-y: auto;
    font-size: 12px;
    padding-left: 20px;
    color: #000;
    .active{
      color: #fff !important;
      background-color: #2872e0;
      &.ant-form-item{
        color: #fff;
      }
    }
    .system{
      padding: 4px 0 ;
      text-align: center;
      cursor: pointer;
      font-size: 14px;
      .fr {
        float: right;
        position: relative;
        top: 4px;
        right: 4px;
        visibility: hidden;
      }
      &:hover {
        .fr {
          visibility: visible;
        }
      }
    }
    &::-webkit-scrollbar {
      width: 3px;
      height: 1px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 3px;
      background: @primary-color;
    }

    &::-webkit-scrollbar-track {
      -webkit-box-shadow: inset 0 0 1px rgba(0, 0, 0, 0);
      border-radius: 3px;
      background: @primary-3;
    }
  }
  .steps-body {
    max-height: calc(100vh - 240px);
    height: calc(100vh - 240px);

    // 同步配置组容器样式
    border: 1px solid #e5e6e8;
    margin: 10px 0;
    padding: 20px 6% 0;
    background: #fff;  // 新增背景色
    border-radius: 8px;  // 新增圆角
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);  // 新增阴影

    .footer {
      height: 64px;
      display: flex;
      justify-content: center;
      align-items: center;
      button {
        width: 86px;
      }
      /deep/
      .ant-btn.ant-btn-loading:not(.ant-btn-circle):not(.ant-btn-circle-outline):not(.ant-btn-icon-only) {
        padding-left: 20px;
      }
    }
  }
}

.config-group {
  // 容器样式保持
  margin-bottom: 0;
  background: #ffffff;
  border-radius: 6px;
  overflow: visible;  // 关键修改：允许子元素溢出
  transition: all 0.3s ease;

  .group-title {
    // 背景圆角实现
    border-radius: 6px 6px 0 0;
    background: #F7F9FC;
    // 创建独立背景层
    position: relative;
    z-index: 1;

    // 背景色圆角裁剪
    &::after {
      content: "";
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: inherit;
      border-radius: inherit;
      z-index: -1;  // 背景层置于内容下方
    }

    // 小蓝条修复
    &::before {
      content: "";
      position: absolute;
      left: 16px;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 16px;
      background: #1890ff;
      border-radius: 2px;
      z-index: 2;  // 确保在背景层之上
    }

    // 其他样式保持
    padding: 12px 40px 12px 36px;
    color: #303133;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    transition: background 0.3s;

    .arrow {
      position: absolute !important;
      right: 40px;  /* 修改这里：箭头往左移动24px */
      top: 50%;
      transform: translateY(-50%) rotate(0deg);
      transition: transform 0.3s ease;
      z-index: 3;  /* 提升箭头的 z-index，确保它在最上层 */
      color: #909399;
      &.arrow-up {
        transform: translateY(-50%) rotate(90deg);  /* 点击后箭头旋转 */
      }
    }

    // 状态样式
    &:hover {
      background: #F2F6FC;
    }
    &.active {
      background: #E0F7FF;
    }
  }

  > div {
    // 内容区域样式
    border-radius: 0 0 6px 6px;
    // 其他内容样式不变...
  }
}

.footer {
  margin-top: 24px;
  padding: 16px 0 0;
  border-top: 1px solid #EBEEF5;
  text-align: right;

  .ant-btn-primary {
    background: #1890ff;
    border-color: #1890ff;
    border-radius: 4px;
    padding: 0 24px;
    height: 36px;
    box-shadow: 0 2px 6px rgba(24, 144, 255, 0.2);
    transition: all 0.3s;

    &:hover {
      background: #40a9ff;
      border-color: #40a9ff;
      transform: translateY(-1px);
    }
  }
}

</style>
