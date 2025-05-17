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
 * @Date: 2022-10-27
 * @FilePath: \datasophon-ui\src\pages\serviceManage\components\ServiceConfig.vue
-->
<template>
  <div>
    <div class="service-config-container">    <!-- 顶部过滤器栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <span class="filter-label">版本</span>
        <a-select 
          :value="currentVersion" 
          @change="changeVersion"
          style="width: 200px"
          class="filter-select"
        >
          <a-select-option 
            v-for="(versionItem, childIndex) in verSionList" 
            :key="childIndex" 
            :value="versionItem.version"
          >
            <a-tooltip v-if="versionItem.description || versionItem.editTime" placement="right">
              <template slot="title">
                <div v-if="versionItem.description">备注: {{ versionItem.description }}</div>
                <div v-if="versionItem.editTime">编辑时间: {{ versionItem.editTime }}</div>
                <div v-if="versionItem.editor">编辑者: {{ versionItem.editor }}</div>
              </template>
              <span>V{{ versionItem.version }}{{ versionItem.isCurrent ? ' (当前)' : '' }}</span>
            </a-tooltip>
            <span v-else>V{{ versionItem.version }}{{ versionItem.isCurrent ? ' (当前)' : '' }}</span>
          </a-select-option>
        </a-select>
      </div>
      
      <div class="filter-item">
        <span class="filter-label">配置组</span>
        <a-select 
          :value="currentId" 
          @change="changeConfigGroup"
          style="width: 150px"
          class="filter-select"
        >
          <a-select-option 
            v-for="item in GroupList" 
            :key="item.id" 
            :value="item.id"
          >
            {{ item.roleGroupName }}
          </a-select-option>
        </a-select>
      </div>
      
      <div class="filter-item">
        <a-tooltip placement="bottom">
          <template slot="title">
            <div class="filter-tooltip">
              <div class="filter-tooltip-title">属性过滤器</div>
              <div class="filter-tooltip-content">通过属性名称、值或描述输入关键字来过滤属性。</div>
            </div>
          </template>
          <a-select 
            :value="filterValue" 
            @change="handleFilter"
            style="width: 150px"
            class="filter-select"
            placeholder="筛选中"
          >
            <a-select-option value="all">全部</a-select-option>
            <a-select-option value="modified">修改过的</a-select-option>
          </a-select>
        </a-tooltip>
      </div>
    </div>
    
    <!-- 配置组区域 - 扁平化设计 -->
    <div class="config-area">
      <a-spin :spinning="loading">
        <div 
          v-for="(group, groupName) in templateData"
          :key="groupName"
          class="config-panel"
        >
          <div 
            class="panel-header" 
            @click="toggleGroup(groupName)"
          >
            {{ convertGroupName(groupName) }}
            <a-icon 
              :type="isGroupExpanded[groupName] ? 'up' : 'right'" 
              class="toggle-icon" 
            />
          </div>
          
          <div v-show="isGroupExpanded[groupName]" class="panel-content">
            <CommonTemplate
                :ref="`template_${groupName}`"
                :steps4Data="steps4Data"
                :templateData="group"
            />
          </div>
        </div>
        
        <div class="footer">
          <a-button type="primary" @click="showSaveDialog">保存</a-button>
        </div>
      </a-spin>
    </div>
    </div>

    <!-- 添加备注弹框 -->
    <a-modal
      v-model="saveDialogVisible"
      title="保存配置"
      :maskClosable="false"
      @ok="confirmSave"
      okText="确定"
      cancelText="取消"
    >
      <div>
        <a-form-item label="备注" :labelCol="{ span: 4 }" :wrapperCol="{ span: 20 }">
          <a-textarea
            v-model="configDescription"
            placeholder="请输入配置备注信息"
            :rows="4"
            style="width: 100%"
          />
        </a-form-item>
      </div>
    </a-modal>
  </div>
</template>
<script>
import CommonTemplate from "@/components/commonTemplate/index";
import {mapActions, mapState} from "vuex";
import RenameGroup from "../renameGroup.vue";
import {getServiceName} from "@/utils/util";
import _ from 'lodash';

export default {
  name: "ServiceConfig",
  components: { CommonTemplate },
  props: {
    steps4Data: Object,
    serviceId: {
      type: [String, Number],
      required: true
    },
    serviceName: {
      type: String,
      default: ''
    }
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
      // 新增过滤相关数据
      filterValue: 'all',
      // 新增备注弹框相关数据
      saveDialogVisible: false,
      configDescription: '',
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
      let serviceName = getServiceName(this.serviceId);

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
    // 新增配置组切换方法
    changeConfigGroup(val) {
      this.currentId = val;
      this.getConfigVersion();
    },
    // 新增过滤方法
    handleFilter(val) {
      this.filterValue = val;
      // 此处可以实现过滤逻辑
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

        // 8. 构建保存参数
        const saveParam = {
          clusterId: this.clusterId,
          serviceName: this.serviceName,
          serviceConfig: JSON.stringify(filterParam),
          roleGroupId: this.currentId,
          description: this.configDescription
        };

        // 9. 提交保存
        const res = await this.$axiosPost(global.API.saveServiceConfig, saveParam);
        if (res.code === 200) {
          this.$message.success("保存成功");
          this.getConfigVersion();
          this.saveDialogVisible = false;
          this.configDescription = '';
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
        serviceInstanceId: this.serviceId,
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
        serviceInstanceId: this.serviceId,
        roleGroupId: JSON.stringify(this.currentId) || '',
      };
      this.$axiosPost(global.API.getConfigVersion, params).then((res) => {
        if (res.code === 200) {
          this.verSionList = res.data;
          if (this.verSionList.length > 0) {
            this.currentVersion = this.verSionList[0].version;
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
        serviceInstanceId: this.serviceId,
        page: 1,
        pageSize: 10000,
        "version": this.currentVersion || '',
        "roleGroupId": JSON.stringify(this.currentId) || '',
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
            || 'General'; // 空值处理

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
      if (!('General' in result)) {
        result.General = [];
      }

      return result;
    },
    // 添加配置组名称转换方法
    convertGroupName(groupName) {

      
      // 处理前缀类型
      if (groupName.startsWith('advanced_')) {
        // 提取配置文件名称
        const configFile = groupName.replace('advanced_', '');
        return `高级 ${configFile}`;
      } else if (groupName.startsWith('custom_')) {
        // 提取配置文件名称
        const configFile = groupName.replace('custom_', '');
        return `自定义 ${configFile}`;
      }
      
      // 默认返回原始名称
      return groupName;
    },
    // 显示保存对话框
    showSaveDialog() {
      this.saveDialogVisible = true;
    },
    
    // 确认保存配置
    confirmSave() {
      this.handleSubmit();
    },
  },
  mounted() {
    this.getServiceRoleType()
  },
  created() {
    console.log('ServiceConfig 创建, serviceId:', this.serviceId);
    console.log('serviceName:', this.serviceName);
  }
};
</script>
<style lang="less" scoped>
.service-config-container {
  position: relative;
  padding: 0;
  height: 100%;
  overflow-y: auto;
  background-color: #fff;
}

/* 过滤器栏样式 */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding: 10px 0;
  background-color: #fff;
  
  .filter-item {
    display: flex;
    align-items: center;
    
    .filter-label {
      margin-right: 8px;
      color: rgba(0, 0, 0, 0.85);
      font-size: 14px;
    }
  }
  
  .filter-select {
    /deep/ .ant-select-selection {
      background-color: #fff;
      border: 1px solid #d9d9d9;
      border-radius: 2px;
      height: 32px;
      
      &:hover {
        border-color: #40a9ff;
      }
      
      .ant-select-selection__rendered {
        line-height: 30px;
      }
    }
  }
}

/* 悬浮提示样式 */
/deep/ .filter-tooltip {
  .filter-tooltip-title {
    font-weight: 500;
    margin-bottom: 4px;
  }
  
  .filter-tooltip-content {
    color: rgba(255, 255, 255, 0.85);
    font-size: 12px;
  }
}

/* 配置面板样式 - 扁平化设计 */
.config-area {
  background-color: #fff;
  
  .config-panel {
    border-top: 1px solid #e8e8e8;
    background-color: #fff;
    margin-bottom: 10px;
    
    &:last-child {
      border-bottom: 1px solid #e8e8e8;
    }
    
    .panel-header {
      padding: 16px;
      background-color: #fff;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: normal;
      color: rgba(0, 0, 0, 0.85);
      transition: all 0.3s;
      
      &:hover {
        background-color: #fafafa;
      }
      
      .toggle-icon {
        color: rgba(0, 0, 0, 0.45);
        font-size: 12px;
        transition: transform 0.2s;
      }
    }
    
    .panel-content {
      padding: 16px;
      background-color: #f5f7fa;
      border-top: 1px solid #e8e8e8;
      
      /deep/ .ant-form-item {
        margin-bottom: 14px;
        
        &:last-child {
          margin-bottom: 0;
        }
      }
    }
  }
}

/* 底部按钮区域 */
.footer {
  margin-top: 20px;
  padding: 16px 0;
  text-align: right;
  
  button {
    min-width: 80px;
    height: 32px;
    font-size: 14px;
  }
}
</style> 