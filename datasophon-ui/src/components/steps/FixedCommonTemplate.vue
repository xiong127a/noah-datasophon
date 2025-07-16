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
  <div class="common-template steps" :class="{'orb-animation-active': isAnimationActive}">
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
            <a-form-item v-for="(child, childIndex) in item.value" style="margin-bottom: 0px" :key="childIndex" :required="item.required && !item.name.endsWith('node_port_mappings') && !item.name.endsWith('cluster_port_mappings')" v-bind="childIndex === 0 ? labelCol : formItemLayoutWithOutLabel" :label="childIndex === 0 || item.value.length === 0  ? item.label : ''">
              <a-row type="flex" class="port-mapping-row" style="position: relative; margin-bottom: 16px; align-items: center; justify-content: space-between; width: 100%;">
                <!-- 左侧输入框 -->
                <a-col :span="10" class="port-input-left" style="display: flex; flex-direction: column; justify-content: flex-end; padding-right: 20px;">
                  <div v-if="childIndex === 0" class="port-label-wrapper">
                    <div class="port-label-left">
                      <template v-if="item.configType === 'custom'">配置名</template>
                      <template v-else-if="item.name.endsWith('node_port_mappings')">容器端口</template>
                      <template v-else-if="item.name.endsWith('cluster_port_mappings')">集群内部端口</template>
                      <template v-else-if="item.name.endsWith('load_balancer_port_mappings')">容器端口</template>
                      <template v-else>键</template>
                    </div>
                  </div>
                  <a-form-item style="width:100%; margin-bottom: 0;">
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
                      <div style="position: relative; overflow: visible;">
                      <a-input v-decorator="[
                      `${item.name+'arrayWithKey'+childIndex}`,
                      {
                      validateTrigger: ['change', 'blur'],
                      initialValue: child.key,
                      rules: [
                        {
                          required: !item.name.endsWith('node_port_mappings') && !item.name.endsWith('cluster_port_mappings') && !item.name.endsWith('load_balancer_port_mappings') ? item.required : false,
                          whitespace: true,
                          message: `${item.label}不能为空!`,
                        },
                      ],
                    }
                      ]" :placeholder="item.configType === 'custom' ? '请输入配置名' : (item.name.endsWith('node_port_mappings') ? '容器内部端口' : (item.name.endsWith('cluster_port_mappings') ? '集群内部端口' : (item.name.endsWith('load_balancer_port_mappings') ? '容器内部端口' : '请输入键')))" class="container-port-input" />
                      </div>
                    </a-tooltip>
                  </a-form-item>
                </a-col>
                
                <!-- 独立的箭头列 -->
                <a-col :span="4" class="arrow-column" v-if="item.name.endsWith('node_port_mappings') || item.name.endsWith('cluster_port_mappings') || item.name.endsWith('load_balancer_port_mappings')">
                  <div class="enhanced-arrow-container" style="transform: translateX(-60px);">
                    <div class="enhanced-arrow-line">
                      <div class="enhanced-flow-effect"></div>
                    </div>
                    <div class="enhanced-arrow-head"></div>
                  </div>
                </a-col>
                
                <!-- 如果不是端口映射，则不显示箭头，但保持布局 -->
                <a-col :span="4" class="arrow-column" v-else>
                </a-col>
                
                <!-- 右侧输入框 -->
                <a-col :span="10" class="port-input-right" style="display: flex; flex-direction: column; justify-content: flex-start; padding-left: 20px;">
                  <div v-if="childIndex === 0" class="port-label-wrapper">
                    <div class="port-label-right">
                      <template v-if="item.configType === 'custom'">配置值</template>
                      <template v-else-if="item.name.endsWith('node_port_mappings')">节点端口</template>
                      <template v-else-if="item.name.endsWith('cluster_port_mappings')">集群端口</template>
                      <template v-else-if="item.name.endsWith('load_balancer_port_mappings')">负载均衡器端口</template>
                      <template v-else>值</template>
                    </div>
                  </div>
                  <a-form-item style="width:100%; margin-bottom: 0; position: relative;">
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
                      <div style="position: relative; overflow: visible;">
                        <div style="display: flex; align-items: center; width: 100%;">
                          <a-input v-decorator="[
                            `${item.name+'arrayWithValue'+childIndex}`,
                            {
                              validateTrigger: ['change', 'blur'],
                              initialValue: child.value,
                              rules: [
                                {
                                  required: !item.name.endsWith('node_port_mappings') && !item.name.endsWith('cluster_port_mappings') && !item.name.endsWith('load_balancer_port_mappings') ? item.required : false,
                                  whitespace: true,
                                  message: `${item.label}不能为空!`,
                                },
                              ],
                            }
                          ]" :placeholder="item.configType === 'custom' ? '请输入配置值' : (item.name.endsWith('node_port_mappings') ? '节点暴露端口' : (item.name.endsWith('cluster_port_mappings') ? '集群端口' : (item.name.endsWith('load_balancer_port_mappings') ? '负载均衡器端口' : '请输入值')))" class="nodeport-input" style="flex: 1;" />
                          
                          <!-- 内嵌删除按钮 -->
                          <a-button 
                            v-if="item.value.length > 1" 
                            type="danger" 
                            shape="circle" 
                            size="small" 
                            icon="minus" 
                            class="inline-delete-button" 
                            @click="() => reduceMultiple(item.name, childIndex, 'multipleWithKey')" 
                          />
                        </div>
                      </div>
                    </a-tooltip>
                    
                    <!-- 没有tooltip时的输入框和按钮 -->
                    <div v-if="!item.description" style="position: relative; overflow: visible;">
                      <div style="display: flex; align-items: center; width: 100%;">
                        <a-input v-decorator="[
                          `${item.name+'arrayWithValue'+childIndex}`,
                          {
                            validateTrigger: ['change', 'blur'],
                            initialValue: child.value,
                            rules: [
                              {
                                required: !item.name.endsWith('node_port_mappings') && !item.name.endsWith('cluster_port_mappings') && !item.name.endsWith('load_balancer_port_mappings') ? item.required : false,
                                whitespace: true,
                                message: `${item.label}不能为空!`,
                              },
                            ],
                          }
                        ]" :placeholder="item.configType === 'custom' ? '请输入配置值' : (item.name.endsWith('node_port_mappings') ? '节点暴露端口' : (item.name.endsWith('cluster_port_mappings') ? '集群端口' : (item.name.endsWith('load_balancer_port_mappings') ? '负载均衡器端口' : '请输入值')))" class="nodeport-input" style="flex: 1;" />
                        
                        <!-- 内嵌删除按钮 -->
                        <a-button 
                          v-if="item.value.length > 1" 
                          type="danger" 
                          shape="circle" 
                          size="small" 
                          icon="minus" 
                          class="inline-delete-button" 
                          @click="() => reduceMultiple(item.name, childIndex, 'multipleWithKey')" 
                        />
                      </div>
                    </div>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-form-item>

            <!-- 添加按钮移回各自部分内 -->
            <a-form-item class="form-multiple-item" :wrapper-col="formItemLayoutWithOutLabel.wrapperCol">
              <a-button type="link" :class="['add-field-button', item.name.endsWith('node_port_mappings') ? 'add-node-port-btn' : (item.name.endsWith('cluster_port_mappings') ? 'add-cluster-port-btn' : (item.name.endsWith('load_balancer_port_mappings') ? 'add-load-balancer-port-btn' : ''))]" @click="() => addMultiple(item.name, 'multipleWithKey')">
                <span class="custom-plus-icon">+</span> 
                <template v-if="item.configType === 'custom'">添加自定义配置</template>
                <template v-else-if="item.name.endsWith('node_port_mappings')">添加NodePort端口映射</template>
                <template v-else-if="item.name.endsWith('cluster_port_mappings')">添加集群内部端口</template>
                <template v-else-if="item.name.endsWith('load_balancer_port_mappings')">添加负载均衡器端口映射</template>
                <template v-else>添加键值对</template>
              </a-button>
            </a-form-item>

            <!-- 只在NodePort映射和负载均衡器端口映射的末尾添加分隔线 -->
            <div v-if="item.name.endsWith('node_port_mappings') || item.name.endsWith('load_balancer_port_mappings')" class="separator-line"></div>
            
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
      animationStartTime: Date.now(),
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
      isAnimationActive: false,
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
  mounted() {
    // 记录动画开始时间
    this.animationStartTime = Date.now();
    // 在组件挂载后启动动画
    this.$nextTick(() => {
      this.isAnimationActive = true;
    });
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
    getAnimationDelay() {
      const now = Date.now();
      const elapsed = now - this.animationStartTime;
      const cycleDuration = 3000; // 3秒一个周期
      const remaining = cycleDuration - (elapsed % cycleDuration);
      return `${remaining}ms`;
    },
    resetOrbAnimations() {
      // 获取所有光球元素
      const orbs = document.querySelectorAll('.port-mapping-orb');
      
      // 移除动画类
      orbs.forEach(orb => {
        const style = orb.style;
        const animation = style.animation;
        style.animation = 'none';
        
        // 触发重排
        void orb.offsetWidth;
        
        // 重新添加动画
        style.animation = animation;
      });
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
      
      // 等待DOM更新后重置所有光球动画
      this.$nextTick(() => {
        this.resetOrbAnimations();
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
    
    /deep/ .container-port-input,
    /deep/ .container-port-form-item .ant-input {
      border-color: #1890ff !important;
      background-color: rgba(24, 144, 255, 0.05) !important;
    }
    
    /deep/ .container-port-input:hover,
    /deep/ .container-port-input:focus,
    /deep/ .container-port-form-item .ant-input:hover,
    /deep/ .container-port-form-item .ant-input:focus {
      border-color: #40a9ff !important;
      box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
    }
    
    /deep/ .nodeport-input,
    /deep/ .nodeport-form-item .ant-input {
      border-color: #52c41a !important;
      background-color: rgba(82, 196, 26, 0.05) !important;
    }
    
    /deep/ .nodeport-input:hover,
    /deep/ .nodeport-input:focus,
    /deep/ .nodeport-form-item .ant-input:hover,
    /deep/ .nodeport-form-item .ant-input:focus {
      border-color: #73d13d !important;
      box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2) !important;
    }
    
    .reduce-icon {
      margin-left: 10px;
      color: #ff4d4f;
      font-size: 20px;
      cursor: pointer;
    }
    
    .delete-icon-wrapper {
      position: absolute;
      right: -44px; /* 调整删除图标位置 */
      top: 50%;
      transform: translateY(-50%);
      height: 32px;
      width: 30px;
      z-index: 20;
      display: flex;
      align-items: center;
      justify-content: center;
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

/* 端口映射相关样式 */
.port-mapping-labels {
  display: flex;
  margin-bottom: 8px;
  padding: 0;
  width: 100%;
}

.port-label-container {
  margin-bottom: 5px;
  height: auto;
  position: absolute;
  top: -28px;  /* 增加与输入框的距离 */
  width: 100%;
}

.port-label-container-left {
  left: 0;
  text-align: left;
  padding-left: 0;
}

.port-label-container-right {
  left: 0;
  text-align: left;
  padding-left: 0;
}

.port-label-left, .port-label-right {
  display: inline-block;
}

.port-label-left {
  color: #1890ff;
  font-size: 13px;
  font-weight: 500;
}

.port-label-right {
  color: #52c41a;
  font-size: 13px;
  font-weight: 500;
}

/* 光球动画相关样式 */
/* 
.port-mapping-orb {
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: var(--orb-color, #1890ff);
  box-shadow: 0 0 5px var(--orb-color, #1890ff),
              0 0 8px var(--orb-color, #1890ff);
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
}

.left-orb {
  right: 8px;
  animation: left-orb-moving 3s infinite ease-in-out;
}

.right-orb {
  left: 8px;
  animation: right-orb-moving 3s infinite ease-in-out;
}

@keyframes left-orb-moving {
  0%, 50%, 100% {
    opacity: 0;
    right: 80%;
  }
  5%, 45% {
    opacity: 1;
  }
  5% {
    right: 80%;
  }
  45% {
    right: 8px;
  }
  50% {
    right: 0;
    opacity: 0;
  }
}

@keyframes right-orb-moving {
  0%, 50%, 100% {
    opacity: 0;
    left: 0;
  }
  55%, 95% {
    opacity: 1;
  }
  55% {
    left: 0;
  }
  95% {
    left: 90%;
    opacity: 1;
  }
  100% {
    left: 90%;
    opacity: 0;
  }
}
*/

/* 全局动画控制类 */
/*
.orb-animation-active .port-mapping-orb {
  animation-play-state: running;
}
*/

/* 清理其他箭头相关样式，但保留我们需要的样式 */
.port-arrow-wrapper,
.arrow-container,
.arrow-container-first,
.arrow-line,
.arrow-head,
.arrow-animation,
.port-arrow-inline,
.arrow-head-inline,
.arrow-wrapper,
.arrow-line-connector,
.arrow-head-connector,
.arrow-absolute-container,
.arrow-absolute-line,
.arrow-absolute-head,
.arrow-absolute-animation {
  display: none;
}

.port-mapping-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 32px;
  background-color: transparent;
  transition: all 0.3s;
  border-radius: 4px;
}

.port-mapping-arrow:hover {
  background-color: rgba(0, 0, 0, 0.03);
}

.port-arrow-icon {
  color: #8c8c8c;
  font-size: 16px;
  animation: arrow-pulse 2s infinite;
}

@keyframes arrow-pulse {
  0% {
    opacity: 0.7;
    transform: scale(0.95);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
  100% {
    opacity: 0.7;
    transform: scale(0.95);
  }
}

// 删除旧的箭头样式，添加新的细长箭头和流动动画

.port-mapping-arrow-container {
  width: 80%;
  height: 4px;
  display: flex;
  align-items: center;
}

.port-mapping-arrow-line {
  height: 2px;
  width: 100%;
  background-color: rgba(24, 144, 255, 0.5);
  position: relative;
  overflow: hidden;
  flex: 1;
}

.arrow-flow-effect {
  position: absolute;
  top: 0;
  height: 100%;
  width: 20px;
  background: linear-gradient(to right, rgba(24, 144, 255, 0), rgba(24, 144, 255, 0.8), rgba(24, 144, 255, 0));
  animation: flow-animation 1.5s infinite linear;
}

.port-mapping-arrow-head {
  width: 0;
  height: 0;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-left: 8px solid rgba(24, 144, 255, 0.5);
  margin-left: 0;
}

@keyframes flow-animation {
  0% {
    left: -20px;
  }
  100% {
    left: 100%;
  }
}

// 修复表单项对齐问题
.form-item-container .ant-form-item {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.form-item-container .ant-row {
  width: 100%;
}

// 确保所有表单项高度一致
.form-item-container input.ant-input {
  height: 32px;
}

// 添加port-mapping-row的样式
.port-mapping-row {
  position: relative;
  min-height: 32px;
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  width: calc(100% - 40px); /* 给删除图标留出空间 */
}

.port-input-left, .port-input-right {
  position: relative;
  z-index: 2;
}

.arrow-column {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0;
}

.port-label-container {
  margin-bottom: 5px;
  padding-left: 10px;
  height: auto;
  position: absolute;
  top: -28px;  /* 增加与输入框的距离 */
  left: 0;
}

// 重新添加增强版箭头样式
.enhanced-arrow-container {
  width: 100%;
  height: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.enhanced-arrow-line {
  height: 3px;
  width: 100%;
  background-color: rgba(24, 144, 255, 0.4);
  position: relative;
  overflow: hidden;
  flex: 1;
  box-shadow: 0 0 3px rgba(24, 144, 255, 0.5);
  border-radius: 1.5px;
}

.enhanced-flow-effect {
  position: absolute;
  top: 0;
  height: 100%;
  width: 50px;
  background: linear-gradient(to right, rgba(24, 144, 255, 0), rgba(24, 144, 255, 1), rgba(24, 144, 255, 0));
  animation: enhanced-flow-animation 1s infinite linear;
  box-shadow: 0 0 15px rgba(24, 144, 255, 0.9);
  filter: blur(0.5px);
}

.enhanced-arrow-head {
  width: 0;
  height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 12px solid rgba(24, 144, 255, 0.8);
  margin-left: 0;
  filter: drop-shadow(0 0 4px rgba(24, 144, 255, 0.9));
}

@keyframes enhanced-flow-animation {
  0% {
    left: -50px;
    opacity: 0.7;
  }
  50% {
    opacity: 1;
  }
  100% {
    left: 100%;
    opacity: 0.7;
  }
}

// 添加内联删除按钮样式
.inline-delete-button {
  margin-left: 8px;
  width: 24px !important;
  height: 24px !important;
  min-width: 24px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  z-index: 100 !important;
  opacity: 1 !important;
  visibility: visible !important;
  position: relative !important;
  background-color: #ff4d4f !important;
  border-color: #ff4d4f !important;
}

.inline-delete-button:hover {
  background-color: #ff7875 !important;
  border-color: #ff7875 !important;
}

.inline-delete-button i {
  font-size: 12px !important;
  line-height: 1 !important;
}

// 隐藏旧的删除按钮样式
.delete-button-container, 
.delete-mapping-btn,
.delete-btn,
.delete-icon-absolute,
.delete-icon-wrapper-container,
.delete-icon-wrapper,
.inline-delete-btn {
  display: none !important;
}

// 添加虚线分隔符样式
.separator-line {
  width: 100%;
  height: 1px;
  border-top: 1px dashed #d9d9d9;
  margin: 16px 0 24px 0;
}

// 添加端口映射按钮样式
.add-node-port-btn {
  color: #52c41a !important;
  font-weight: 500;
  border-bottom: 1px dashed #52c41a;
  margin-bottom: 10px;
}

.add-node-port-btn .custom-plus-icon {
  background-color: #52c41a !important;
  color: #ffffff !important;
}

.add-node-port-btn:hover {
  color: #73d13d !important;
  border-bottom-color: #73d13d;
}

.add-node-port-btn:hover .custom-plus-icon {
  background-color: #73d13d !important;
}

.add-cluster-port-btn {
  color: #52c41a !important;
  font-weight: 500;
  border-bottom: 1px dashed #52c41a;
  margin-bottom: 10px;
}

.add-cluster-port-btn .custom-plus-icon {
  background-color: #52c41a !important;
  color: #ffffff !important;
}

.add-cluster-port-btn:hover {
  color: #73d13d !important;
  border-bottom-color: #73d13d;
}

.add-cluster-port-btn:hover .custom-plus-icon {
  background-color: #73d13d !important;
}

// 自定义加号图标通用样式
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
  line-height: 15px;
}

// 单独的删除按钮容器
.delete-button-container {
  position: absolute;
  right: -40px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1000;
  display: block !important;
}

.delete-mapping-btn {
  width: 24px;
  height: 24px;
  font-size: 14px;
  display: flex !important;
  align-items: center;
  justify-content: center;
}

.port-label-wrapper {
  margin-bottom: 5px;
  width: 100%;
}

.port-label-left {
  color: #1890ff;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
}

.port-label-right {
  color: #52c41a;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
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

/* 端口映射相关全局样式 */
.container-port-form-item .ant-input,
.container-port-input {
  border-color: #1890ff !important;
  
  &:hover, &:focus {
    border-color: #40a9ff !important;
  }
}

.nodeport-form-item .ant-input,
.nodeport-input {
  border-color: #52c41a !important;
  
  &:hover, &:focus {
    border-color: #73d13d !important;
  }
}

/* 端口映射动画效果 */
.port-mapping-arrow-container {
  .port-mapping-arrow {
    .arrow-line {
      position: relative;
      overflow: hidden;
      
      &:after {
        content: '';
        position: absolute;
        height: 100%;
        width: 10px;
        background-color: rgba(255, 255, 255, 0.5);
        left: -10px;
        top: 0;
        animation: flowing 1.2s infinite linear;
      }
    }
  }
}

@keyframes flowing {
  0% {
    left: -10px;
  }
  100% {
    left: 100%;
  }
}
</style> 