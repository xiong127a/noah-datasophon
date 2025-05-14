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
 * @LastEditTime: 2023-04-13 15:15:13
 * @FilePath: \ddh-ui\src\components\commonTemplate\index.vue
-->
<template>
  <div class="common-template steps">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="form-content mgh160">
      <div v-for="(item, index) in testData" :key="index">
        <div class="form-item-container" v-if="!['multipleWithKey', 'multiple', 'multipleSelect'].includes(item.type)">
          <a-form-item :label="item.label">
            <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
              <template slot="title">
                <div class="tooltip-content">
                  <div class="tooltip-header">
                    <span class="tooltip-label">{{item.label}}</span>
                    <span class="tooltip-name">{{item.name.replaceAll("!", ".")}}</span>
                  </div>
                  <div class="tooltip-description">{{item.description}}</div>
                </div>
              </template>
              <a-input v-if="item.type==='input'" v-decorator="[
              `${item.name}`,
              // { validator: checkName }
              { initialValue: item.value+'',rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
            ]" placeholder="请输入" />
            </a-tooltip>
            <a-input v-if="item.type==='input' && !item.description" v-decorator="[
            `${item.name}`,
            // { validator: checkName }
            { initialValue: item.value+'',rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
          ]" placeholder="请输入" />
            
            <a-tooltip v-if="item.type==='slider' && item.description" placement="right" overlayClassName="custom-tooltip">
              <template slot="title">
                <div class="tooltip-content">
                  <div class="tooltip-header">
                    <span class="tooltip-label">{{item.label}}</span>
                    <span class="tooltip-name">{{item.name.replaceAll("!", ".")}}</span>
                  </div>
                  <div class="tooltip-description">{{item.description}}</div>
                </div>
              </template>
              <a-slider :marks="marks(item)" :min="item.minValue" :max="item.maxValue" style="width: 96%;display: inline-block" v-decorator="[`${item.name}`,{initialValue: item.value? Number(item.value) : 0}]" />
            </a-tooltip>
            <a-slider v-if="item.type==='slider' && !item.description" :marks="marks(item)" :min="item.minValue" :max="item.maxValue" style="width: 96%;display: inline-block" v-decorator="[`${item.name}`,{initialValue: item.value? Number(item.value) : 0}]" />
            
            <a-tooltip v-if="item.type==='switch' && item.description" placement="right" overlayClassName="custom-tooltip">
              <template slot="title">
                <div class="tooltip-content">
                  <div class="tooltip-header">
                    <span class="tooltip-label">{{item.label}}</span>
                    <span class="tooltip-name">{{item.name.replaceAll("!", ".")}}</span>
                  </div>
                  <div class="tooltip-description">{{item.description}}</div>
                </div>
              </template>
              <a-switch v-decorator="[`${item.name}`, { valuePropName: 'checked', initialValue: item.value }]"></a-switch>
            </a-tooltip>
            <a-switch v-if="item.type==='switch' && !item.description" v-decorator="[`${item.name}`, { valuePropName: 'checked', initialValue: item.value }]"></a-switch>
            
            <a-tooltip v-if="item.type==='select' && item.description" placement="right" overlayClassName="custom-tooltip">
              <template slot="title">
                <div class="tooltip-content">
                  <div class="tooltip-header">
                    <span class="tooltip-label">{{item.label}}</span>
                    <span class="tooltip-name">{{item.name.replaceAll("!", ".")}}</span>
                  </div>
                  <div class="tooltip-description">{{item.description}}</div>
                </div>
              </template>
              <a-select v-decorator="[
              `${item.name}`,
              {initialValue:item.value, rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
              ]" placeholder="请选择">
                <a-select-option v-for="(child, childIndex) in item.selectValue" :key="childIndex" :value="child">{{child}}</a-select-option>
              </a-select>
            </a-tooltip>
            <a-select v-if="item.type==='select' && !item.description" v-decorator="[
            `${item.name}`,
            {initialValue:item.value, rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
            ]" placeholder="请选择">
              <a-select-option v-for="(child, childIndex) in item.selectValue" :key="childIndex" :value="child">{{child}}</a-select-option>
            </a-select>
          </a-form-item>
          <div class="filed-name-tips">
            <span class="filed-name-tips-word" :title="item.name">{{item.name.replaceAll("!", ".")}}</span>
          </div>
        </div>
        <div v-else>
          <div v-if="['multiple'].includes(item.type)" class="form-item-container">
            <a-form-item v-for="(child, childIndex) in item.value" :key="childIndex" v-bind="childIndex === 0 ? labelCol : formItemLayoutWithOutLabel" :label="(childIndex === 0 || item.value.length === 0) ? item.label : ''">
              <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
                <template slot="title">
                  <div class="tooltip-content">
                    <div class="tooltip-header">
                      <span class="tooltip-label">{{item.label}}</span>
                      <span class="tooltip-name">{{item.name.replaceAll("!", ".")}}</span>
                    </div>
                    <div class="tooltip-description">{{item.description}}</div>
                  </div>
                </template>
                <a-input v-decorator="[
                  `${item.name+'multiple'+childIndex}`,
                  {
                  validateTrigger: ['change', 'blur'],
                  initialValue: child,
                  rules: [
                    {
                      required: item.required,
                      whitespace: true,
                      message: `${item.label}不能为空!`,
                    },
                  ],
                }
                ]" placeholder="请输入" />
              </a-tooltip>
              <a-input v-if="!item.description" v-decorator="[
                `${item.name+'multiple'+childIndex}`,
                {
                validateTrigger: ['change', 'blur'],
                initialValue: child,
                rules: [
                  {
                    required: item.required,
                    whitespace: true,
                    message: `${item.label}不能为空!`,
                  },
                ],
              }
              ]" placeholder="请输入" />
              <span @click="() => reduceMultiple(item.name, childIndex, 'multiple')">
                <svg-icon v-if="item.value.length > 1" icon-class="reduce-icon" class="reduce-icon" />
              </span>
            </a-form-item>
            <a-form-item class="form-multiple-item" v-bind="item.value.length === 0 ? labelCol : formItemLayoutWithOutLabel" :label="item.value.length === 0 ? item.label : ''">
              <a-button v-if="item.isCustom || item.configLevel === 'custom' || (item.name && item.name.toLowerCase().includes('custom'))" type="dashed" @click="() => addMultiple(item.name, 'multiple')">
                <a-icon type="plus" />Add field
              </a-button>
            </a-form-item>
            <div class="filed-name-tips">
              <span class="filed-name-tips-word" :title="item.name">{{item.name.replaceAll("!", ".")}}</span>
            </div>
          </div>
          <div v-if="['multipleWithKey'].includes(item.type)" class="form-item-container">
            <a-form-item v-for="(child, childIndex) in item.value" style="margin-bottom: 0px" :key="childIndex" :required="item.required" v-bind="childIndex === 0 ? labelCol : formItemLayoutWithOutLabel" :label="childIndex === 0 || item.value.length === 0  ? item.label : ''">
              <a-row type="flex" style="position: relative">
                <a-col :span="12">
                  <a-form-item style="width:97%">
                    <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
                      <template slot="title">
                        <div class="tooltip-content">
                          <div class="tooltip-header">
                            <span class="tooltip-label">{{item.label}}</span>
                            <span class="tooltip-name">{{item.name.replaceAll("!", ".")}}</span>
                          </div>
                          <div class="tooltip-description">{{item.description}}</div>
                        </div>
                      </template>
                      <a-input v-decorator="[
                      `${item.name+'arrayWithKey'+childIndex}`,
                      {
                      validateTrigger: ['change', 'blur'],
                      initialValue: child.key,
                      rules: [
                        {
                          required: item.required,
                          whitespace: true,
                          message: `${item.label}不能为空!`,
                        },
                      ],
                    }
                    ]" placeholder="请输入" />
                    </a-tooltip>
                    <a-input v-if="!item.description" v-decorator="[
                    `${item.name+'arrayWithKey'+childIndex}`,
                    {
                    validateTrigger: ['change', 'blur'],
                    initialValue: child.key,
                    rules: [
                      {
                        required: item.required,
                        whitespace: true,
                        message: `${item.label}不能为空!`,
                      },
                    ],
                  }
                  ]" placeholder="请输入" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item style="width:97%">
                    <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
                      <template slot="title">
                        <div class="tooltip-content">
                          <div class="tooltip-header">
                            <span class="tooltip-label">{{item.label}}</span>
                            <span class="tooltip-name">{{item.name.replaceAll("!", ".")}}</span>
                          </div>
                          <div class="tooltip-description">{{item.description}}</div>
                        </div>
                      </template>
                      <a-input v-decorator="[
                      `${item.name+'arrayWithValue'+childIndex}`,
                      {
                      validateTrigger: ['change', 'blur'],
                      initialValue: child.value,
                      rules: [
                        {
                          required: item.required,
                          whitespace: true,
                          message: `${item.label}不能为空!`,
                        },
                      ],
                    }
                    ]" placeholder="请输入" />
                    </a-tooltip>
                    <a-input v-if="!item.description" v-decorator="[
                    `${item.name+'arrayWithValue'+childIndex}`,
                    {
                    validateTrigger: ['change', 'blur'],
                    initialValue: child.value,
                    rules: [
                      {
                        required: item.required,
                        whitespace: true,
                        message: `${item.label}不能为空!`,
                      },
                    ],
                  }
                  ]" placeholder="请输入" />
                  </a-form-item>
                </a-col>
                <span style="position: absolute; right: 0px" @click="() => reduceMultiple(item.name, childIndex, 'multipleWithKey')">
                  <svg-icon v-if="item.value.length > 1" icon-class="reduce-icon" class="reduce-icon" />
                </span>
              </a-row>
            </a-form-item>
            <a-form-item class="form-multiple-item" v-bind="item.value.length === 0 ? labelCol : formItemLayoutWithOutLabel" :label="item.value.length === 0 ? item.label : ''">
              <a-button v-if="item.isCustom || item.configLevel === 'custom' || (item.name && item.name.toLowerCase().includes('custom'))" type="dashed" @click="() => addMultiple(item.name, 'multipleWithKey')">
                <a-icon type="plus" />Add field
              </a-button>
            </a-form-item>
            <div class="filed-name-tips">
              <span class="filed-name-tips-word" :title="item.name">{{item.name.replaceAll("!", ".")}}</span>
            </div>
          </div>
          <div v-if="['multipleSelect'].includes(item.type)" class="form-item-container">
            <a-form-item :label="item.label">
              <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
                <template slot="title">
                  <div class="tooltip-content">
                    <div class="tooltip-header">
                      <span class="tooltip-label">{{item.label}}</span>
                      <span class="tooltip-name">{{item.name.replaceAll("!", ".")}}</span>
                    </div>
                    <div class="tooltip-description">{{item.description}}</div>
                  </div>
                </template>
                <a-select mode="multiple" v-decorator="[`${item.name}`, {initialValue:item.value, rules: [{ required: item.required, message: `${item.label}不能为空!` }] },]" placeholder="请选择">
                  <a-select-option v-for="(child, childIndex) in item.selectValue" :key="childIndex" :value="child">{{child}}</a-select-option>
                </a-select>
              </a-tooltip>
              <a-select v-if="!item.description" mode="multiple" v-decorator="[`${item.name}`, {initialValue:item.value, rules: [{ required: item.required, message: `${item.label}不能为空!` }] },]" placeholder="请选择">
                <a-select-option v-for="(child, childIndex) in item.selectValue" :key="childIndex" :value="child">{{child}}</a-select-option>
              </a-select>
            </a-form-item>
            <div class="filed-name-tips">
              <span class="filed-name-tips-word" :title="item.name">{{item.name.replaceAll("!", ".")}}</span>
            </div>
          </div>
        </div>
      </div>
    </a-form>
  </div>
</template>
<script>
export default {
  name: "CommonTemplate",
  components: {},
  props: { templateData: Array },
  data() {
    const self = this
    return {
      testData: this.templateData,
      labelCol: {
        xs: { span: 24 },
        sm: { span: 7 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 17 },
      },
      initFormFiledFlag: false,
      formItemLayoutWithOutLabel: {
        wrapperCol: {
          xs: { span: 24, offset: 0 },
          sm: { span: 17, offset: 7 },
        },
      },
      form: this.$form.createForm(this, {
        onValuesChange: function (props, fileds) {
          if (self.initFormFiledFlag) {
            self.initFormFiledFlag = false
            return false
          }
          for (var i in fileds) {
            if (i.includes('multiple') || i.includes('arrayWithKey') || i.includes('arrayWithValue')) {
              console.log(fileds, 'sssss')
              let splitArr = i.includes('multiple') ? i.split('multiple') :  i.includes('arrayWithKey') ? i.split('arrayWithKey') : i.split('arrayWithValue')
              const name = splitArr[0]
              let formData = self.testData
              formData.forEach(item => {
                if (item.name === name) {
                  // item.value
                  if (i.includes('multiple')) {
                    item.value[Number(splitArr[1])] = fileds[i]
                  }
                  if (i.includes('arrayWithKey')) {
                    item.value[Number(splitArr[1])].key = fileds[i]
                  }
                  if (i.includes('arrayWithValue')) {
                    item.value[Number(splitArr[1])].value = fileds[i]
                  }
                }
              })
              self.initFormFiledFlag = true
              self.testData = formData
              self.form.getFieldsValue([`${i}`])
              self.form.setFieldsValue({
                [`${i}`]: fileds[i]
              })
            }
          }
        },
      }),
    };
  },
  watch: {
    templateData: {
      handler(val) {
        this.testData = val;
        this.initFormData();
      },
      deep: true,
      immediate: true,
    },
  },
  methods: {
    initFormData() {
      let arr = _.cloneDeep(this.testData);
      let formData = arr.filter((item) => !item.hidden);
      formData.forEach((item, index) => {
        if (["multipleWithKey", "multiple"].includes(item.type)) {
          if (item.value.length === 0) {
            if (["multipleWithKey"].includes(item.type)) {
              item.value.push({
                key: "",
                value: "",
              });
            } else {
              item.value.push("");
            }
          } else {
            if (["multipleWithKey"].includes(item.type)) {
              let arr = [];
              item.value.map((childItem, childIndex) => {
                for (let key in childItem) {
                  arr.push({
                    key: key,
                    value: childItem[key],
                  });
                }
              });
              item.value = arr;
            }
          }
        } else {
          item.value = [null, undefined, ""].includes(item.value)
            ? item.defaultValue
            : item.value;
          if (Object.prototype.toString.call(item.value) === '[object Array]' && item.value.length === 0) {
            item.value = item.defaultValue
          }
        }
      });
      this.testData = formData;
    },
    marks(item) {
      return {
        [`${item.minValue}`]: item.minValue,
        [`${item.maxValue}`]: item.maxValue,
      };
    },
    addMultiple(name, type) {
      this.testData.forEach((item) => {
        if (item.name === name) {
          if (["multipleWithKey"].includes(type)) {
            item.value.push({
              key: "",
              value: "",
            });
          } else {
            item.value.push("");
          }
        }
      });
    },
    reduceMultiple(name, childIndex, type) {
      this.testData.forEach((item) => {
        if (item.name === name) {
          item.value.splice(childIndex, 1);
          var obj = {}
          if (item.type === 'multipleWithKey') {
            item.value.map((child, childIndex) => {
              obj[`${item.name+'arrayWithKey'+childIndex}`]= child.key
              obj[`${item.name+'arrayWithValue'+childIndex}`]= child.value
            })
          }
          if (item.type === 'multiple') {
            item.value.map((child, childIndex) => {
              obj[`${item.name+'multiple'+childIndex}`]= child
            })
          }
          var keys = Object.keys(obj)
          this.form.getFieldsValue([...keys])
          this.form.setFieldsValue({
            ...obj
          })
        }
      });
    },
  },
  created() {
    // this.initFormData();
  },
};
</script>
<style lang="less" scoped>
.common-template {
  .mgh160 {
    // margin: 0 110px;
  }
  
  /deep/ .custom-tooltip {
    .ant-tooltip-inner {
      background-color: white;
      color: rgba(0, 0, 0, 0.85);
      padding: 0;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      overflow: hidden;
      
      .tooltip-content {
        .tooltip-header {
          padding: 10px 12px;
          background-color: #f5f7fa;
          border-bottom: 1px solid #e4e7ed;
          
          .tooltip-label {
            font-weight: 500;
            color: rgba(0, 0, 0, 0.85);
          }
          
          .tooltip-name {
            display: block;
            margin-top: 2px;
            color: #1890ff;
            font-family: monospace;
            font-size: 12px;
          }
        }
        
        .tooltip-description {
          padding: 10px 12px;
          color: rgba(0, 0, 0, 0.65);
          font-size: 12px;
          line-height: 1.5;
          background-color: white;
        }
      }
    }
    
    .ant-tooltip-arrow::before {
      background-color: #f5f7fa;
    }
  }
  
  .form-content {
    /deep/ .ant-input {
      background-color: #f5f7fa;
      max-width: 360px;
      border: 1px solid #e4e7ed;
      &:hover, &:focus {
        border-color: #40a9ff;
        background-color: #fff;
      }
    }
    
    /deep/ .ant-select-selection {
      background-color: #f5f7fa;
      max-width: 360px;
      border: 1px solid #e4e7ed;
      &:hover {
        border-color: #40a9ff;
        background-color: #fff;
      }
    }
    
    /deep/ .ant-slider {
      max-width: 360px;
    }
    
    /deep/ .ant-switch {
      background-color: #f5f7fa;
      &-checked {
        background-color: #1890ff;
      }
    }
    
    .reduce-icon {
      cursor: pointer;
      font-size: 18px;
      color: @primary-color;
      margin-left: 6px;
    }
    .form-multiple-item:last-child {
      margin-bottom: 12px;
    }
    .form-item-container {
      position: relative;
      // margin-bottom: 6px;
      .filed-name-tips {
        display: flex;
        align-items: center;
        position: absolute;
        font-size: 12px;
        top: 32px;
        left: 12px;
        width: 20%;
        &-word {
          display: inline-block;
          max-width: calc(100% - 0px);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          color: rgba(0, 0, 0, 0.45);
        }
      }
      .filed-name-tips-icon {
          cursor: pointer;
        }
      .slider-icon {
        position: relative;
        top: -28px;
      }
    }
  }
}
</style>

<!-- 添加全局样式覆盖Ant Design默认样式 -->
<style lang="less">
/* 自定义tooltip样式，覆盖Ant Design默认样式 */
html body .custom-tooltip {
  .ant-tooltip-inner {
    background-color: white !important;
    color: rgba(0, 0, 0, 0.85) !important;
    padding: 0 !important;
    border-radius: 4px !important;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15) !important;
    overflow: hidden !important;
    max-width: 300px !important;
    
    .tooltip-content {
      .tooltip-header {
        padding: 10px 12px !important;
        background-color: #f5f7fa !important;
        border-bottom: 1px solid #e4e7ed !important;
        
        .tooltip-label {
          font-weight: 500 !important;
          color: rgba(0, 0, 0, 0.85) !important;
          display: block !important;
        }
        
        .tooltip-name {
          display: block !important;
          margin-top: 2px !important;
          color: #1890ff !important;
          font-family: monospace !important;
          font-size: 12px !important;
        }
      }
      
      .tooltip-description {
        padding: 10px 12px !important;
        color: rgba(0, 0, 0, 0.65) !important;
        font-size: 12px !important;
        line-height: 1.5 !important;
        background-color: white !important;
      }
    }
  }
  
  .ant-tooltip-arrow {
    border-color: #f5f7fa !important;
    
    &:before {
      background-color: #f5f7fa !important;
    }
  }
}
</style>