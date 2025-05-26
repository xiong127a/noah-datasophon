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
-->
<template>
  <div class="common-template steps">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="form-content mgh160">
      <div v-for="(item, index) in testData" :key="index">
        <div class="form-item-container" v-if="!['multipleWithKey', 'multiple', 'multipleSelect'].includes(item.type)">
          <a-form-item :label="item.label">
            <!-- 处理不同的表单类型 -->
            <!-- 输入框类型 -->
            <template v-if="item.type==='input'">
              <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
                <template slot="title">
                  <div class="tooltip-content">
                    <div class="tooltip-header">
                      <span class="tooltip-label">{{item.label}}</span>
                      <span class="tooltip-name" :title="item.name.replaceAll('!', '.')">{{item.name.replaceAll("!", ".")}}</span>
                    </div>
                    <div class="tooltip-description">{{item.description}}</div>
                  </div>
                </template>
                <div class="input-with-unit">
                  <a-textarea v-if="item.heightMultiple && item.heightMultiple > 1" v-decorator="[
                    `${item.name}`,
                    { initialValue: item.value+'', rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
                  ]" placeholder="请输入" :style="getInputHeightStyle(item)" :rows="item.heightMultiple" />
                  <a-input v-else v-decorator="[
                    `${item.name}`,
                    { initialValue: item.value+'', rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
                  ]" placeholder="请输入" />
                  <span v-if="item.unit" class="input-unit-suffix" :style="getUnitHeightStyle(item)">{{item.unit}}</span>
                </div>
              </a-tooltip>
              
              <div v-else class="input-with-unit">
                <a-textarea v-if="item.heightMultiple && item.heightMultiple > 1" v-decorator="[
                  `${item.name}`,
                  { initialValue: item.value+'', rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
                ]" placeholder="请输入" :style="getInputHeightStyle(item)" :rows="item.heightMultiple" />
                <a-input v-else v-decorator="[
                  `${item.name}`,
                  { initialValue: item.value+'', rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
                ]" placeholder="请输入" />
                <span v-if="item.unit" class="input-unit-suffix" :style="getUnitHeightStyle(item)">{{item.unit}}</span>
              </div>
            </template>
            
            <!-- 滑块类型 -->
            <template v-else-if="item.type==='slider'">
              <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
                <template slot="title">
                  <div class="tooltip-content">
                    <div class="tooltip-header">
                      <span class="tooltip-label">{{item.label}}</span>
                      <span class="tooltip-name" :title="item.name.replaceAll('!', '.')">{{item.name.replaceAll("!", ".")}}</span>
                    </div>
                    <div class="tooltip-description">{{item.description}}</div>
                  </div>
                </template>
                <div>
                  <div class="input-with-unit">
                    <a-input 
                      v-decorator="[
                        `${item.name}_value`,
                        { initialValue: item.value+'', rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
                      ]" 
                      placeholder="请输入" 
                      style="width: 100%" 
                    />
                    <span v-if="item.unit" class="input-unit-suffix">{{item.unit}}</span>
                  </div>
                  <a-slider 
                    :marks="marks(item)" 
                    :min="item.minValue" 
                    :max="item.maxValue" 
                    style="width: 96%;display: inline-block; margin-top: 8px;" 
                    v-decorator="[`${item.name}`,{initialValue: item.value? Number(item.value) : 0}]"
                  />
                </div>
              </a-tooltip>
              
              <div v-else>
                <div class="input-with-unit">
                  <a-input 
                    v-decorator="[
                      `${item.name}_value`,
                      { initialValue: item.value+'', rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
                    ]" 
                    placeholder="请输入" 
                    style="width: 100%" 
                  />
                  <span v-if="item.unit" class="input-unit-suffix">{{item.unit}}</span>
                </div>
                <a-slider 
                  :marks="marks(item)" 
                  :min="item.minValue" 
                  :max="item.maxValue" 
                  style="width: 96%;display: inline-block; margin-top: 8px;" 
                  v-decorator="[`${item.name}`,{initialValue: item.value? Number(item.value) : 0}]"
                />
              </div>
            </template>
            
            <!-- 开关类型 -->
            <template v-else-if="item.type==='switch'">
              <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
                <template slot="title">
                  <div class="tooltip-content">
                    <div class="tooltip-header">
                      <span class="tooltip-label">{{item.label}}</span>
                      <span class="tooltip-name" :title="item.name.replaceAll('!', '.')">{{item.name.replaceAll("!", ".")}}</span>
                    </div>
                    <div class="tooltip-description">{{item.description}}</div>
                  </div>
                </template>
                <a-switch v-decorator="[`${item.name}`, { valuePropName: 'checked', initialValue: item.value }]"></a-switch>
              </a-tooltip>
              
              <a-switch v-else v-decorator="[`${item.name}`, { valuePropName: 'checked', initialValue: item.value }]"></a-switch>
            </template>
            
            <!-- 选择类型 -->
            <template v-else-if="item.type==='select'">
              <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
                <template slot="title">
                  <div class="tooltip-content">
                    <div class="tooltip-header">
                      <span class="tooltip-label">{{item.label}}</span>
                      <span class="tooltip-name" :title="item.name.replaceAll('!', '.')">{{item.name.replaceAll("!", ".")}}</span>
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
              
              <a-select v-else v-decorator="[
              `${item.name}`,
              {initialValue:item.value, rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
              ]" placeholder="请选择">
                <a-select-option v-for="(child, childIndex) in item.selectValue" :key="childIndex" :value="child">{{child}}</a-select-option>
              </a-select>
            </template>
            
            <!-- 其他未识别的类型 -->
            <template v-else>
              <a-tooltip v-if="item.description" placement="right" overlayClassName="custom-tooltip">
                <template slot="title">
                  <div class="tooltip-content">
                    <div class="tooltip-header">
                      <span class="tooltip-label">{{item.label}}</span>
                      <span class="tooltip-name" :title="item.name.replaceAll('!', '.')">{{item.name.replaceAll("!", ".")}}</span>
                    </div>
                    <div class="tooltip-description">{{item.description}}</div>
                  </div>
                </template>
                <a-input v-decorator="[
                `${item.name}`,
                  { initialValue: item.value+'', rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
              ]" placeholder="请输入" />
              </a-tooltip>
              
              <a-input v-else v-decorator="[
              `${item.name}`,
                { initialValue: item.value+'', rules: [{ required: item.required, message: `${item.label}不能为空!` }] },
            ]" placeholder="请输入" />
            </template>
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
                      <span class="tooltip-name" :title="item.name.replaceAll('!', '.')">{{item.name.replaceAll("!", ".")}}</span>
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
            <a-form-item class="form-multiple-item" :wrapper-col="formItemLayoutWithOutLabel.wrapperCol">
              <a-button type="link" class="add-field-button" @click="() => addMultiple(item.name, 'multiple')">
                <span class="custom-plus-icon">+</span> 添加属性
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
                            <span class="tooltip-name" :title="item.name.replaceAll('!', '.')">{{item.name.replaceAll("!", ".")}}</span>
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
                            <span class="tooltip-name" :title="item.name.replaceAll('!', '.')">{{item.name.replaceAll("!", ".")}}</span>
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
            <a-form-item class="form-multiple-item" :wrapper-col="formItemLayoutWithOutLabel.wrapperCol">
              <a-button type="link" class="add-field-button" @click="() => addMultiple(item.name, 'multipleWithKey')">
                <span class="custom-plus-icon">+</span> 添加属性
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
                      <span class="tooltip-name" :title="item.name.replaceAll('!', '.')">{{item.name.replaceAll("!", ".")}}</span>
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
  name: "FixedCommonTemplate",
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
      sliderValues: {}, // 存储slider的值，用于双向绑定
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
          
          // 处理slider输入框和滑块的双向绑定
          for (var i in fileds) {
            // 处理slider值的变化
            if (i.endsWith('_value')) {
              const sliderName = i.replace('_value', '');
              // 如果是从输入框更新的值，同步到滑块
              if (!isNaN(Number(fileds[i]))) {
                self.form.setFieldsValue({
                  [sliderName]: Number(fileds[i])
                });
              }
            } else if (self.testData.some(item => item.name === i && item.type === 'slider')) {
              // 如果是从滑块更新的值，同步到输入框
              self.form.setFieldsValue({
                [`${i}_value`]: fileds[i] + ''
              });
            }
          
            // 原有的多重值处理逻辑
            if (i.includes('multiple') || i.includes('arrayWithKey') || i.includes('arrayWithValue')) {
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
    getInputHeightStyle(item) {
      if (item.heightMultiple && item.heightMultiple > 1) {
        const heightValue = `${32 * item.heightMultiple}px`;
        return {
          height: heightValue,
          resize: 'none',
          'padding-top': '8px',
          'padding-bottom': '8px'
        };
      }
      return {};
    },
    getUnitHeightStyle(item) {
      if (item.heightMultiple && item.heightMultiple > 1) {
        const heightValue = `${32 * item.heightMultiple}px`;
        return {
          height: heightValue,
          display: 'flex',
          'align-items': 'center'
        };
      }
      return {};
    },
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
  width: 100%;
  background-color: transparent;
  
  .mgh160 {
    margin-bottom: 0px;
  }
  
  .form-content {
    position: relative;
    background-color: #f5f7fa;
    border-radius: 4px;
    padding: 12px;
    
    /deep/ .ant-input {
      background-color: #ffffff;
      border-radius: 4px;
    }
    /deep/ .ant-slider-track {
      background: #1890ff;
    }
    /deep/ .ant-checkbox-checked .ant-checkbox-inner {
      background-color: rgb(19, 94, 249);
      }
    /deep/ .ant-select-selection {
      background-color: #ffffff;
    }
    
    .reduce-icon {
      margin-left: 10px;
      color: #ff4d4f;
      font-size: 20px;
      cursor: pointer;
    }
    
    .form-multiple-item {
      margin-bottom: 8px !important;
      margin-top: -4px;
      padding-left: 7px;
      display: flex;
      align-items: center;
      
      .add-field-button {
        padding: 0;
        margin: 0;
        color: #1890ff;
        font-size: 13px;
        height: 24px;
        line-height: 24px;
        border: none;
        box-shadow: none;
        display: flex;
        align-items: center;
        
        &:hover, &:focus {
          color: #40a9ff;
          background: transparent;
          
          .custom-plus-icon {
            background-color: #40a9ff;
            color: white;
      }
    }
    
        .custom-plus-icon {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          font-size: 14px;
          font-weight: bold;
          margin-right: 5px;
          width: 16px;
          height: 16px;
          border-radius: 50%;
          background-color: #e6f7ff;
          color: #1890ff;
          line-height: 15px;
    }
      }
    }
    
    .form-item-container {
      position: relative;
      
      .filed-name-tips {
        display: none;
        position: absolute;
        width: 300px;
        max-width: 300px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        bottom: -21px;
        right: 0;
        text-align: right;
        color: #8a8e99;
        font-size: 12px;
        cursor: pointer;
      }
     
      .filed-name-tips-word {
        display: inline-block;
      }
    }
  }
}

.add-field-button {
  display: none;
}

.form-multiple-item {
  display: none;
}

.tooltip-content {
  max-width: 100%;
  width: 100%;
        }

.tooltip-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 8px;
}

.tooltip-label {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 5px;
        }

.tooltip-name {
  font-size: 12px;
  color: #1890ff;
  background: rgba(24, 144, 255, 0.1);
  padding: 2px 5px;
  border-radius: 2px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: break-all;
}

.tooltip-description {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  line-height: 1.5;
}

.input-with-unit {
        position: relative;
  display: flex;
  align-items: flex-start;
  width: 100%;

  .ant-input {
    border-top-right-radius: 0;
    border-bottom-right-radius: 0;
    border-right: none;
  }
  
  .ant-input-textarea {
    flex: 1;
    
    .ant-input {
      border-top-right-radius: 0;
      border-bottom-right-radius: 0;
      border-right: none;
      height: 100%;
    }
  }
}

.input-unit-suffix {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 70px;
  height: 32px;
  padding: 0 11px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  text-align: center;
  background-color: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-left: none;
  border-radius: 0 4px 4px 0;
  align-self: stretch; /* 拉伸以匹配输入框高度 */
}

/* 悬停时的样式 */
.input-with-unit:hover .input-unit-suffix {
  border-color: #40a9ff;
}

/* 输入框获取焦点时的样式 - 使用兼容性更好的选择器 */
.input-with-unit .ant-input:focus + .input-unit-suffix {
  border-color: #40a9ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 确保输入框仍然占据主要空间 */
.input-with-unit .ant-input {
  flex-grow: 1;
}
</style>

<!-- 添加全局样式覆盖Ant Design默认样式 -->
<style lang="less">
/* 自定义tooltip样式，覆盖Ant Design默认样式 */
html body .custom-tooltip {
  max-width: 350px;
  
  .ant-tooltip-inner {
    background-color: white !important;
    color: rgba(0, 0, 0, 0.85) !important;
    padding: 0 !important;
    border-radius: 4px !important;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15) !important;
    overflow: hidden !important;
    width: 100% !important;
    max-width: 100% !important;
    
    .tooltip-content {
      width: 100%;
      .tooltip-header {
        padding: 10px 12px !important;
        background-color: #f5f7fa !important;
        border-bottom: 1px solid #e4e7ed !important;
        
        .tooltip-label {
          font-weight: 500 !important;
          color: rgba(0, 0, 0, 0.85) !important;
          display: block !important;
          margin-bottom: 5px !important;
        }
        
        .tooltip-name {
          display: block !important;
          width: 100% !important;
          color: #1890ff !important;
          font-family: monospace !important;
          font-size: 12px !important;
          overflow: hidden !important;
          text-overflow: ellipsis !important;
          white-space: nowrap !important;
          word-break: break-all !important;
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