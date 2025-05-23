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
      <!-- 对比模式下的过滤器 -->
      <template v-if="compareMode">
        <div class="version-compare-container">
          <div class="version-compare-row">
            <span class="compare-text">Comparing Changes in:</span>
            
            <!-- 第一个版本选择器 -->
            <a-dropdown :trigger="['click']" overlayClassName="version-dropdown">
              <a-button style="width: 120px" class="filter-select">
                {{ currentVersion !== undefined ? `版本 ${currentVersion}` : '选择版本' }}
                <a-icon type="down" />
              </a-button>
              <div slot="overlay" class="version-list-container">
                <div class="version-list-header">
                  <a-input-search 
                    placeholder="搜索" 
                    style="width: 100%"
                    @change="onVersionSearchChange"
                    v-model="versionSearchKeyword"
                  />
                </div>
                <div class="version-list">
                  <div 
                    v-for="(versionItem, index) in filteredVersionList" 
                    :key="index"
                    class="version-item"
                    :class="{ 'version-item-active': currentVersion === versionItem.version }"
                  >
                    <div class="version-item-content" @click="changeVersion(versionItem.version)">
                      <div class="version-item-header">
                        <span class="version-number">版本 {{ versionItem.version }}</span>
                        <span v-if="versionItem.isCurrent" class="version-tag">当前使用</span>
                      </div>
                      <div class="version-item-description">{{ versionItem.description }}</div>
                      <div class="version-item-footer">
                        <span>{{ versionItem.editor }}</span>
                        <span>编辑于 {{ versionItem.editTime }}</span>
                      </div>
                      <a-icon v-if="currentVersion === versionItem.version" type="check" class="version-selected-icon" />
                    </div>
                  </div>
                </div>
              </div>
            </a-dropdown>
            
            <span class="compare-text">with</span>
            
            <!-- 第二个版本选择器 -->
            <a-dropdown :trigger="['click']" overlayClassName="version-dropdown">
              <a-button style="width: 120px" class="filter-select">
                {{ compareVersion !== undefined ? `版本 ${compareVersion}` : '选择版本' }}
                <a-icon type="down" />
              </a-button>
              <div slot="overlay" class="version-list-container">
                <div class="version-list-header">
                  <a-input-search 
                    placeholder="搜索" 
                    style="width: 100%"
                    @change="onCompareVersionSearchChange"
                    v-model="compareVersionSearchKeyword"
                  />
                </div>
                <div class="version-list">
                  <div 
                    v-for="(versionItem, index) in filteredCompareVersionList" 
                    :key="index"
                    class="version-item"
                    :class="{ 'version-item-active': compareVersion === versionItem.version }"
                  >
                    <div class="version-item-content" @click="changeCompareVersion(versionItem.version)">
                      <div class="version-item-header">
                        <span class="version-number">版本 {{ versionItem.version }}</span>
                        <span v-if="versionItem.isCurrent" class="version-tag">当前使用</span>
                      </div>
                      <div class="version-item-description">{{ versionItem.description }}</div>
                      <div class="version-item-footer">
                        <span>{{ versionItem.editor }}</span>
                        <span>编辑于 {{ versionItem.editTime }}</span>
                      </div>
                      <a-icon v-if="compareVersion === versionItem.version" type="check" class="version-selected-icon" />
                    </div>
                  </div>
                </div>
              </div>
            </a-dropdown>
            
            <!-- 关闭对比按钮 -->
            <a-button 
              type="link" 
              class="close-compare-btn"
              @click="closeCompareMode"
            >
              <a-icon type="close" />
            </a-button>
          </div>
        </div>
        
        <!-- 角色组和筛选放在下面一行 -->
        <div class="filter-controls-row">
          <div class="filter-item">
            <a-dropdown :trigger="['click']" overlayClassName="config-group-dropdown">
              <a-button style="width: 200px" class="filter-select">
                {{ currentId !== undefined && getGroupName(currentId) ? `角色组 ${getGroupName(currentId)}` : '角色组' }}
                <a-icon type="down" />
              </a-button>
              <div slot="overlay" class="config-group-list-container">
                <div class="config-group-list">
                  <div 
                    v-for="item in GroupList" 
                    :key="item.id"
                    class="config-group-item"
                    :class="{ 'config-group-item-active': currentId === item.id }"
                    @click="changeConfigGroup(item.id)"
                  >
                    <div class="config-group-item-header">
                      <span class="config-group-name">角色组 {{ item.roleGroupName }}</span>
                    </div>
                    <a-icon v-if="currentId === item.id" type="check" class="config-group-selected-icon" />
                  </div>
                </div>
              </div>
            </a-dropdown>
          </div>
          
          <!-- 添加显示模式选择 -->
          <div class="filter-item">
            <a-radio-group 
              v-model="showOnlyDifferences" 
              @change="handleShowModeChange"
              buttonStyle="solid"
              size="small"
            >
              <a-radio-button :value="true">只显示差异</a-radio-button>
              <a-radio-button :value="false">显示全部</a-radio-button>
            </a-radio-group>
          </div>
          
          <div class="filter-item">
            <a-input-search
              v-model="searchKeyword"
              placeholder="筛选中"
              style="width: 200px"
              class="filter-select"
              @search="handleSearch"
              @change="handleInputChange"
              allowClear
            />
          </div>
        </div>
      </template>
      
      <!-- 非对比模式下的过滤器 -->
      <template v-else>
        <div class="filter-item">
          <a-dropdown :trigger="['click']" overlayClassName="version-dropdown">
            <a-button style="width: 200px" class="filter-select">
              {{ currentVersion !== undefined ? `版本 ${currentVersion}` : '选择版本' }}
              <a-icon type="down" />
            </a-button>
            <div slot="overlay" class="version-list-container">
              <div class="version-list-header">
                <a-input-search 
                  placeholder="搜索" 
                  style="width: 100%"
                  @change="onVersionSearchChange"
                  v-model="versionSearchKeyword"
                />
              </div>
              <div class="version-list">
                <div 
                  v-for="(versionItem, index) in filteredVersionList" 
                  :key="index"
                  class="version-item"
                  :class="{ 'version-item-active': currentVersion === versionItem.version }"
                >
                  <div class="version-item-content" @click="changeVersion(versionItem.version)">
                    <div class="version-item-header">
                      <span class="version-number">版本 {{ versionItem.version }}</span>
                      <span v-if="versionItem.isCurrent" class="version-tag">当前使用</span>
                    </div>
                    <div class="version-item-description">{{ versionItem.description }}</div>
                    <div class="version-item-footer">
                      <span>{{ versionItem.editor }}</span>
                      <span>编辑于 {{ versionItem.editTime }}</span>
                    </div>
                    <a-icon v-if="currentVersion === versionItem.version" type="check" class="version-selected-icon" />
                  </div>
                  <!-- 添加对比按钮 -->
                  <a-tooltip title="对比当前版本" placement="top">
                    <div 
                      v-if="currentVersion !== versionItem.version" 
                      class="version-compare-btn"
                      @click="startCompareWithVersion(versionItem.version)"
                    >
                      <a-icon type="swap" />
                    </div>
                  </a-tooltip>
                </div>
              </div>
            </div>
          </a-dropdown>
        </div>
        
        <div class="filter-item">
          <a-dropdown :trigger="['click']" overlayClassName="config-group-dropdown">
            <a-button style="width: 200px" class="filter-select">
              {{ currentId !== undefined && getGroupName(currentId) ? `角色组 ${getGroupName(currentId)}` : '角色组' }}
              <a-icon type="down" />
            </a-button>
            <div slot="overlay" class="config-group-list-container">
              <div class="config-group-list">
                <div 
                  v-for="item in GroupList" 
                  :key="item.id"
                  class="config-group-item"
                  :class="{ 'config-group-item-active': currentId === item.id }"
                  @click="changeConfigGroup(item.id)"
                >
                  <div class="config-group-item-header">
                    <span class="config-group-name">角色组 {{ item.roleGroupName }}</span>
                  </div>
                  <a-icon v-if="currentId === item.id" type="check" class="config-group-selected-icon" />
                </div>
              </div>
            </div>
          </a-dropdown>
        </div>
        
        <div class="filter-item">
          <a-input-search
            v-model="searchKeyword"
            placeholder="筛选中"
            style="width: 200px"
            class="filter-select"
            @search="handleSearch"
            @change="handleInputChange"
            allowClear
          />
        </div>
      </template>
    </div>
    
    <!-- 配置组区域 - 扁平化设计 -->
    <div class="config-area">
      <a-spin :spinning="loading">
        <!-- 对比模式下的内容 -->
        <template v-if="compareMode && !loading">
          <div class="config-panel compare-header-panel">
            <table class="compare-table">
              <thead>
                <tr class="compare-header-row">
                  <th class="compare-header-cell attribute-header">属性名称</th>
                  <th class="compare-header-cell">
                    <div class="version-header">
                      <span class="version-title">版本 {{ currentVersion }}</span>
                      <span v-if="isCurrentVersionActive" class="version-tag">当前使用</span>
                    </div>
                  </th>
                  <th class="compare-header-cell">
                    <div class="version-header">
                      <span class="version-title">版本 {{ compareVersion }}</span>
                      <span v-if="isCompareVersionActive" class="version-tag">当前使用</span>
                    </div>
                  </th>
                </tr>
              </thead>
            </table>
          </div>
          
          <!-- 按分组显示配置项 -->
          <div 
            v-for="(group, groupName) in filteredCompareData"
            :key="groupName"
            class="config-panel"
          >
            <div 
              class="panel-header" 
              @click="toggleGroup(groupName)"
            >
              {{ groupName }}
              <a-icon 
                :type="isGroupExpanded[groupName] ? 'up' : 'right'" 
                class="toggle-icon" 
              />
            </div>
            
            <div v-show="isGroupExpanded[groupName]" class="panel-content">
              <table class="compare-table">
                <tbody>
                  <tr 
                    v-for="(item, index) in group" 
                    :key="index"
                    class="compare-row-simple"
                    :class="{ 'different': item.isDifferent }"
                  >
                    <td class="compare-cell-simple attribute-cell">{{ item.name }}</td>
                    <td class="compare-cell-simple value-cell">{{ item[currentVersion] }}</td>
                    <td class="compare-cell-simple value-cell">{{ item[compareVersion] }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>
        
        <!-- 非对比模式下的内容 -->
        <template v-else>
          <div 
            v-for="(group, groupName) in filteredTemplateData"
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
        </template>
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
          <div class="placeholder-hint">（如不填写，系统将自动生成包含修改内容的备注）</div>
        </a-form-item>
      </div>
    </a-modal>
  </div>
</template>
<script>
import CommonTemplate from "@/components/steps/FixedCommonTemplate.vue";
import {mapActions, mapState} from "vuex";
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
      compareMode: false,
      compareVersion: undefined,
      compareData: null, // 存储对比数据
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
      searchKeyword: '',
      // 新增备注弹框相关数据
      saveDialogVisible: false,
      configDescription: '',
      placeholderText: '请输入配置备注信息\n（如不填写，系统将自动生成包含修改内容的备注）',
      versionSearchKeyword: '',
      compareVersionSearchKeyword: '',
      isCurrentVersionActive: false, // 当前版本是否为激活版本
      isCompareVersionActive: false, // 对比版本是否为激活版本
      showOnlyDifferences: true, // 是否只显示差异项，默认为true
    };
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
    // 添加计算属性，根据筛选条件过滤配置项
    filteredTemplateData() {
      if (!this.templateData || Object.keys(this.templateData).length === 0) {
        return {};
      }

      // 如果没有搜索关键字，返回原始数据
      if (!this.searchKeyword) {
        return this.templateData;
      }

      const result = {};
      
      // 遍历所有配置组
      Object.entries(this.templateData).forEach(([groupName, configItems]) => {
        // 过滤符合条件的配置项
        const filteredItems = configItems.filter(item => {
          // 搜索label、name、value和description
          const keyword = this.searchKeyword.toLowerCase();
          const label = (item.label || '').toLowerCase();
          const name = (item.name || '').replaceAll('!', '.').toLowerCase();
          const value = String(item.value || '').toLowerCase();
          const description = (item.description || '').toLowerCase();
          
          return label.includes(keyword) || 
                 name.includes(keyword) || 
                 value.includes(keyword) || 
                 description.includes(keyword);
        });
        
        // 如果过滤后有配置项，添加到结果中
        if (filteredItems.length > 0) {
          result[groupName] = filteredItems;
        }
      });
      
      return result;
    },
    filteredVersionList() {
      if (!this.verSionList || this.verSionList.length === 0) {
        return [];
      }

      const keyword = this.versionSearchKeyword.toLowerCase();
      return this.verSionList.filter(item => {
        // 将version转换为字符串再进行小写转换
        const label = String(item.version || '').toLowerCase();
        const description = (item.description || '').toLowerCase();
        
        // 添加过滤条件：在对比模式下过滤掉已选择的compareVersion
        const matchesKeyword = label.includes(keyword) || description.includes(keyword);
        const shouldFilter = this.compareMode && this.compareVersion && item.version === this.compareVersion;
        
        return matchesKeyword && !shouldFilter;
      });
    },
    filteredCompareVersionList() {
      if (!this.verSionList || this.verSionList.length === 0) {
        return [];
      }

      const keyword = this.compareVersionSearchKeyword.toLowerCase();
      // 过滤掉当前已选择的版本，防止自己和自己比较
      return this.verSionList.filter(item => {
        // 将version转换为字符串再进行小写转换
        const label = String(item.version || '').toLowerCase();
        const description = (item.description || '').toLowerCase();
        // 添加过滤条件：排除当前选择的版本
        return (label.includes(keyword) || description.includes(keyword)) && 
               item.version !== this.currentVersion;
      });
    },
    // 添加计算属性，用于过滤对比数据
    filteredCompareData() {
      if (!this.compareData) return {};
      
      // 如果没有搜索关键字，返回原始数据
      if (!this.searchKeyword) {
        return this.compareData;
      }
      
      // 过滤符合搜索条件的配置项
      const keyword = this.searchKeyword.toLowerCase();
      const result = {};
      
      Object.entries(this.compareData).forEach(([groupName, items]) => {
        const filteredItems = items.filter(item => {
          const name = (item.name || '').toLowerCase();
          const valueA = String(item[this.currentVersion] || '').toLowerCase();
          const valueB = String(item[this.compareVersion] || '').toLowerCase();
          
          return name.includes(keyword) || 
                 valueA.includes(keyword) || 
                 valueB.includes(keyword);
        });
        
        if (filteredItems.length > 0) {
          result[groupName] = filteredItems;
        }
      });
      
      return result;
    }
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
      // 重置筛选状态
      this.searchKeyword = '';
      // 折叠所有分组
      Object.keys(this.isGroupExpanded).forEach(groupName => {
        this.$set(this.isGroupExpanded, groupName, false);
      });
      this.getConfigVersion();
    },
    // 新增搜索方法
    handleSearch(value) {
      this.searchKeyword = value;
      // 搜索后自动展开包含匹配项的分组
      this.$nextTick(() => {
        this.expandMatchingGroups();
      });
    },
    
    // 处理输入变化
    handleInputChange(e) {
      this.searchKeyword = e.target.value;
      // 输入变化后自动展开包含匹配项的分组
      this.$nextTick(() => {
        this.expandMatchingGroups();
      });
    },
    
    // 展开包含匹配项的分组
    expandMatchingGroups() {
      // 如果没有搜索关键字，则折叠所有分组
      if (!this.searchKeyword) {
        Object.keys(this.templateData).forEach(groupName => {
          this.$set(this.isGroupExpanded, groupName, false);
        });
        return;
      }
      
      // 获取筛选后的分组
      const filteredGroups = Object.keys(this.filteredTemplateData);
      
      // 展开包含匹配项的分组
      filteredGroups.forEach(groupName => {
        this.$set(this.isGroupExpanded, groupName, true);
      });
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

        // 8. 获取当前用户信息
        const userStr = localStorage.getItem(process.env.VUE_APP_USER_KEY);
        const currentUser = userStr ? JSON.parse(userStr) : {};

        // 9. 构建保存参数
        const saveParam = {
          clusterId: this.clusterId,
          serviceName: this.serviceName,
          serviceConfig: JSON.stringify(filterParam),
          roleGroupId: this.currentId,
          description: this.configDescription,
          userId: currentUser.id, // 添加用户ID
          username: currentUser.username // 添加用户名
        };

        // 10. 提交保存
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
      if (this.currentVersion === val) return;
      this.currentVersion = val;
      
      // 检查是否为当前使用版本
      const versionItem = this.verSionList.find(v => v.version === val);
      if (versionItem) {
        this.isCurrentVersionActive = versionItem.isCurrent;
      }
      
      // 重置筛选状态
      this.searchKeyword = '';
      
      // 折叠所有分组
      Object.keys(this.isGroupExpanded).forEach(groupName => {
        this.$set(this.isGroupExpanded, groupName, false);
      });
      
      if (this.compareMode && this.compareVersion) {
        this.loadCompareData();
      } else {
        this.getServiceConfigOption(true);
      }
    },
    changeCasting(val) {
      console.log(val.target.value);
      this.currentId = val.target.value
      // 重置筛选状态
      this.searchKeyword = '';
      // 折叠所有分组
      Object.keys(this.isGroupExpanded).forEach(groupName => {
        this.$set(this.isGroupExpanded, groupName, false);
      });
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
            
            // 标记当前使用的版本
            const currentVersionItem = this.verSionList.find(v => v.isCurrent);
            if (currentVersionItem) {
              this.isCurrentVersionActive = this.currentVersion === currentVersionItem.version;
            }
            
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
            this.$set(this.isGroupExpanded, name, false);
          } else {
            this.$set(this.isGroupExpanded, name, false);
          }
        });
        
        // 重置筛选状态，确保数据加载后筛选功能正常工作
        this.searchKeyword = '';
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
    onVersionSearchChange(value) {
      this.versionSearchKeyword = value;
    },
    onCompareVersionSearchChange(value) {
      this.compareVersionSearchKeyword = value;
    },
    // 获取配置组名称
    getGroupName(id) {
      if (!this.GroupList || this.GroupList.length === 0) {
        return '';
      }
      const group = this.GroupList.find(item => item.id === id);
      return group ? group.roleGroupName : '';
    },
    handleCompareModeChange(value) {
      this.compareMode = value;
      if (!value) {
        // 退出对比模式时清空对比数据
        this.compareData = null;
      }
    },
    closeCompareMode() {
      this.compareMode = false;
      this.compareData = null;
    },
    changeCompareVersion(version) {
      // 添加验证，防止选择与当前版本相同的版本
      if (version === this.currentVersion) {
        this.$message.warning('不能选择相同的版本进行对比');
        return;
      }
      
      if (this.compareVersion === version) return;
      this.compareVersion = version;
      // 检查是否为当前使用版本
      const versionItem = this.verSionList.find(v => v.version === version);
      if (versionItem) {
        this.isCompareVersionActive = versionItem.isCurrent;
      }
      this.loadCompareData();
    },
    // 加载对比数据
    loadCompareData() {
      if (!this.currentVersion || !this.compareVersion) return;
      
      this.loading = true;
      const params = {
        serviceInstanceId: this.serviceId,
        roleGroupId: JSON.stringify(this.currentId) || '',
        versionA: this.currentVersion,
        versionB: this.compareVersion,
        showOnlyDifferences: this.showOnlyDifferences
      };
      
      this.$axiosPost(global.API.configVersionCompare, params).then(res => {
        if (res.code === 200) {
          // 直接使用后端返回的数据
          this.compareData = res.data;
          
          // 自动展开所有分组
          Object.keys(this.compareData).forEach(groupName => {
            this.$set(this.isGroupExpanded, groupName, true);
          });
        } else {
          this.$message.error(res.msg || '获取对比数据失败');
        }
        this.loading = false;
      }).catch(error => {
        console.error('对比数据加载失败:', error);
        this.$message.error('对比数据加载失败');
        this.loading = false;
      });
    },
    startCompareWithVersion(version) {
      // 添加验证，防止选择与当前版本相同的版本
      if (version === this.currentVersion) {
        this.$message.warning('不能选择相同的版本进行对比');
        return;
      }
      
      this.compareMode = true;
      this.compareVersion = version;
      
      // 检查是否为当前使用版本
      const versionItem = this.verSionList.find(v => v.version === version);
      if (versionItem) {
        this.isCompareVersionActive = versionItem.isCurrent;
      }
      
      this.loadCompareData();
    },
    handleShowModeChange() {
      // 重新加载对比数据
      this.loadCompareData();
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
      min-width: 42px; /* 确保所有标签宽度一致 */
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
    
    /* 添加搜索框样式 */
    /deep/ .ant-select-search {
      width: 100%;
      
      .ant-select-search__field {
        padding: 4px 11px;
        width: 100% !important;
      }
    }
    
    /* 输入框样式 */
    /deep/ .ant-input {
      height: 32px;
      border-radius: 2px;
      
      &:hover {
        border-color: #40a9ff;
      }
      
      &:focus {
        border-color: #40a9ff;
        box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
      }
    }
    
    /* 搜索按钮样式 */
    /deep/ .ant-input-search-button {
      height: 32px;
      border-radius: 0 2px 2px 0;
    }
  }
}

/* 版本对比相关样式 */
.compare-switch {
  margin-right: 8px;
}

.version-compare-container {
  width: 100%;
  margin-bottom: 12px;
}

.version-compare-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
}

.filter-controls-row {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.compare-text {
  color: rgba(0, 0, 0, 0.65);
  white-space: nowrap;
}

.close-compare-btn {
  color: rgba(0, 0, 0, 0.45);
  font-size: 16px;
  
  &:hover {
    color: rgba(0, 0, 0, 0.65);
  }
}

/* 对比模式表头 */
.compare-header {
  display: flex;
  padding: 16px;
  background-color: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 500;
}

.compare-header-item {
  &.attribute {
    flex: 2;
  }
  
  &:not(.attribute) {
    flex: 1;
    text-align: center;
  }
}

.version-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .version-title {
    font-weight: 500;
    margin-bottom: 4px;
  }
}

/* 对比内容样式 */
.compare-header-panel {
  margin-top: 0;
  border-top: none;
  margin-bottom: 16px;
}

.compare-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  
  .compare-header-row {
    background-color: #fafafa;
    border-bottom: 1px solid #e8e8e8;
    
    .compare-header-cell {
      padding: 16px;
      font-weight: 500;
      text-align: left;
      
      &.attribute-header {
        width: 30%;
      }
    }
  }
  
  .compare-row-simple {
    border-bottom: 1px solid #f0f0f0;
    
    &:nth-child(even) {
      background-color: #fafafa;
    }
    
    &:hover {
      background-color: #f5f5f5;
    }
    
    &.different {
      background-color: #fffbe6;
      
      &:hover {
        background-color: #fff8d8;
      }
    }
  }
  
  .compare-cell-simple {
    padding: 12px 16px;
    vertical-align: top;
    word-break: break-all;
    
    &.attribute-cell {
      width: 30%;
      font-weight: 500;
    }
    
    &.value-cell {
      width: 35%;
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

.placeholder-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 版本列表样式 */
.version-dropdown {
  width: 500px;
  
  .ant-dropdown-menu {
    padding: 0;
  }
}

.version-list-container {
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  width: 500px;
}

.version-list-header {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.version-list {
  max-height: 400px;
  overflow-y: auto;
}

.version-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  &:hover {
    background-color: #f5f5f5;
  }
  
  &-active {
    background-color: #e6f7ff;
  }
  
  &-content {
    flex: 1;
    cursor: pointer;
  }
  
  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }
  
  &-description {
    color: rgba(0, 0, 0, 0.65);
    margin-bottom: 8px;
    word-break: break-all;
  }
  
  &-footer {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }
}

.version-number {
  font-weight: 700;
  font-size: 16px;
  color: #1890ff;
}

.version-tag {
  background-color: #52c41a;
  color: #fff;
  padding: 2px 8px;
  border-radius: 2px;
  font-size: 12px;
}

.version-selected-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #1890ff;
  font-size: 16px;
}

.version-compare-btn {
  width: 32px;
  height: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px solid #d9d9d9;
  border-radius: 2px;
  cursor: pointer;
  margin-left: 8px;
  
  &:hover {
    color: #1890ff;
    border-color: #1890ff;
  }
}

/* 配置组下拉列表样式 */
.config-group-dropdown {
  width: 300px;
  
  .ant-dropdown-menu {
    padding: 0;
  }
}

.config-group-list-container {
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  width: 300px;
}

.config-group-list {
  max-height: 300px;
  overflow-y: auto;
}

.config-group-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  position: relative;
  
  &:hover {
    background-color: #f5f5f5;
  }
  
  &-active {
    background-color: #e6f7ff;
  }
  
  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.config-group-name {
  font-weight: 500;
  font-size: 14px;
}

.config-group-selected-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #1890ff;
  font-size: 16px;
}
</style> 