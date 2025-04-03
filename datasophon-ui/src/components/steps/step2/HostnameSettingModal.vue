<template>
  <a-modal
    :visible="visible"
    :title="$t('设置主机名')"
    :width="650"
    :maskClosable="false"
    :destroyOnClose="true"
    @cancel="handleCancel"
    :footer="null"
    class="hostname-setting-modal"
  >
    <div class="hostname-setting-container">
      <!-- 添加功能介绍 -->
      <div class="feature-description">
        <a-alert type="info" show-icon>
          <span slot="message">
            <div class="description-title">{{ $t('功能说明') }}</div>
            <div class="description-content">
              {{ $t('该功能可以批量设置集群主机名，支持自定义前缀、中间数字位数、分隔符和后缀。系统会根据规则自动为每台主机生成唯一的主机名，便于识别和管理。支持最多5位数字编号，可满足10万台主机的命名需求。') }}
            </div>
          </span>
        </a-alert>
      </div>
      
      <div class="hostname-form">
        <a-form :form="form" layout="vertical" @change="updatePreview">
          <a-form-item :label="$t('主机名前缀')" :colon="false">
            <a-select
              v-decorator="[
                'prefix',
                {
                  initialValue: 'bigdata',
                  rules: [{ required: false, message: $t('请选择或输入主机名前缀') }]
                }
              ]"
              :placeholder="$t('请选择或输入主机名前缀')"
              allow-clear
              show-search
              @change="updatePreview"
            >
              <a-select-option v-for="item in prefixOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :label="$t('编号样式')" :colon="false">
            <a-select
              v-decorator="[
                'zeroCount',
                {
                  initialValue: 3,
                  rules: [{ required: false, message: $t('请选择编号样式') }]
                }
              ]"
              :placeholder="$t('请选择编号样式')"
              style="width: 100%"
              @change="updatePreview"
            >
              <a-select-option v-for="item in numberFormatOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :label="$t('分隔符')" :colon="false">
            <a-select
              v-decorator="[
                'separator',
                {
                  initialValue: '',
                  rules: [{ required: false }]
                }
              ]"
              :placeholder="$t('请选择或输入分隔符')"
              allow-clear
              show-search
              @change="updatePreview"
            >
              <a-select-option v-for="item in separatorOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :label="$t('后缀')" :colon="false">
            <a-input
              v-decorator="[
                'suffix',
                {
                  initialValue: '',
                  rules: [{ required: false }]
                }
              ]"
              :placeholder="$t('请输入后缀（可选）')"
              allow-clear
              @change="updatePreview"
            />
          </a-form-item>

          <a-form-item :label="$t('主机名预览')" :colon="false">
            <div class="preview-container">
              <div class="preview-examples">
                <div class="preview-example">{{ previewExample1 }}</div>
                <div class="preview-example">{{ previewExample2 }}</div>
                <div class="preview-example">{{ previewExample3 }}</div>
                <div class="preview-more">...更多</div>
              </div>
              <div class="preview-description">
                {{ $t('实际命名将按上述格式为所有主机依次设置主机名') }}
              </div>
            </div>
          </a-form-item>
        </a-form>
      </div>

      <div class="hostname-actions">
        <a-button @click="handleCancel">{{ $t('取消') }}</a-button>
        <a-button
          type="primary"
          :loading="saveLoading"
          @click="handleSave"
        >{{ $t('保存') }}</a-button>
        <a-button
          type="primary"
          :loading="saveAndSyncLoading"
          @click="handleSaveAndSync"
        >{{ $t('保存并同步hosts') }}</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script>
import HostCheckService from './HostCheckService'

export default {
  name: 'HostnameSettingModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    clusterId: {
      type: [Number, String],
      required: true
    }
  },
  data() {
    return {
      form: this.$form.createForm(this),
      saveLoading: false,
      saveAndSyncLoading: false,
      prefixOptions: [
        { label: 'bigdata', value: 'bigdata' },
        { label: 'hadoop', value: 'hadoop' },
        { label: 'spark', value: 'spark' },
        { label: 'hive', value: 'hive' },
        { label: 'flink', value: 'flink' },
        { label: 'kafka', value: 'kafka' },
        { label: 'datanode', value: 'datanode' },
        { label: 'node', value: 'node' },
        { label: 'server', value: 'server' },
        { label: 'host', value: 'host' }
      ],
      separatorOptions: [
        { label: '无', value: '' },
        { label: '-', value: '-' },
        { label: '_', value: '_' },
        { label: '.', value: '.' }
      ],
      numberFormatOptions: [
        { label: '一位数字 (例如: 1、2...)', value: 1 },
        { label: '两位数字 (例如: 01、02...)', value: 2 },
        { label: '三位数字 (例如: 001、002...)', value: 3 },
        { label: '四位数字 (例如: 0001、0002...)', value: 4 },
        { label: '五位数字 (例如: 00001、00002...)', value: 5 }

      ],
      // 预览示例
      previewExample1: 'bigdata001',
      previewExample2: 'bigdata002',
      previewExample3: 'bigdata003'
    }
  },
  watch: {
    visible(val) {
      if (val) {
        // 表单重置
        this.form.resetFields();
        // 初始化预览
        this.updatePreview();
      }
    }
  },
  mounted() {
    // 初始化表单后更新预览
    this.$nextTick(() => {
      this.updatePreview();
    });
  },
  methods: {
    // 获取表单值
    getFormValues() {
      return new Promise((resolve, reject) => {
        this.form.validateFields((err, values) => {
          if (err) {
            reject(err)
          } else {
            resolve(values)
          }
        })
      })
    },

    // 更新预览示例
    updatePreview() {
      try {
        // 获取当前表单值（不使用异步方法，避免表单验证）
        const formValues = this.form.getFieldsValue();
        const prefix = formValues.prefix || 'bigdata';
        const zeroCount = formValues.zeroCount || 3;
        const separator = formValues.separator || '';
        const suffix = formValues.suffix || '';
        
        // 生成预览示例
        this.previewExample1 = this.generateExampleHostname(prefix, zeroCount, separator, suffix, 1);
        this.previewExample2 = this.generateExampleHostname(prefix, zeroCount, separator, suffix, 2);
        this.previewExample3 = this.generateExampleHostname(prefix, zeroCount, separator, suffix, 3);
      } catch (e) {
        console.error('Update preview error:', e);
      }
    },

    // 生成示例主机名
    generateExampleHostname(prefix, zeroCount, separator, suffix, index) {
      const paddedNum = index.toString().padStart(zeroCount, '0');
      return prefix + separator + paddedNum + suffix;
    },

    // 保存主机名
    async handleSave() {
      try {
        this.saveLoading = true;
        const values = await this.getFormValues();
        
        const res = await HostCheckService.batchSetHostname(
          this,
          this.clusterId,
          values.prefix || 'bigdata',
          values.zeroCount || 3,
          values.separator || '',
          values.suffix || ''
        );
        
        if (res.code === 200) {
          this.$message.success(res.data?.message || this.$t('主机名设置成功'));
          this.$emit('success');
          this.$emit('close');
        } else {
          this.$message.error(res.msg || this.$t('主机名设置失败'));
        }
      } catch (e) {
        console.error('Save error:', e);
      } finally {
        this.saveLoading = false;
      }
    },

    // 保存并同步hosts文件
    async handleSaveAndSync() {
      try {
        this.saveAndSyncLoading = true;
        // 先保存主机名
        const values = await this.getFormValues();
        
        const saveRes = await HostCheckService.batchSetHostname(
          this,
          this.clusterId,
          values.prefix || 'bigdata',
          values.zeroCount || 3,
          values.separator || '',
          values.suffix || ''
        );
        
        if (saveRes.code === 200) {
          // 再同步hosts文件
          this.$emit('syncHosts', this.clusterId);
          this.$emit('success');
          this.$emit('close');
        } else {
          this.$message.error(saveRes.msg || this.$t('主机名设置失败'));
        }
      } catch (e) {
        console.error('Save and sync error:', e);
      } finally {
        this.saveAndSyncLoading = false;
      }
    },

    // 取消
    handleCancel() {
      this.$emit('close');
    }
  }
}
</script>

<style lang="less" scoped>
.hostname-setting-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-description {
  margin-bottom: 8px;
}

.description-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.description-content {
  color: #666;
  line-height: 1.5;
}

.hostname-form {
  padding: 0 8px;
}

.preview-container {
  height: auto;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background-color: #fafafa;
  padding: 16px;
}

.preview-examples {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-example {
  font-family: "Consolas", "Monaco", monospace;
  font-size: 16px;
  color: #0071e3;
  padding: 4px 8px;
  background-color: #f0f8ff;
  border-radius: 4px;
  border-left: 3px solid #0071e3;
}

.preview-more {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
  text-align: center;
  font-style: italic;
}

.preview-description {
  margin-top: 10px;
  font-size: 13px;
  color: #666;
  text-align: center;
}

.hostname-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>

<style>
/* 全局样式，使用苹果风格 */
.hostname-setting-modal .ant-modal-content {
  border-radius: 10px;
  overflow: hidden;
}

.hostname-setting-modal .ant-modal-header {
  background-color: #f5f5f7;
  border-bottom: none;
  padding: 16px 24px;
}

.hostname-setting-modal .ant-modal-title {
  font-weight: 500;
  font-size: 18px;
  color: #1d1d1f;
}

.hostname-setting-modal .ant-modal-body {
  padding: 24px;
  background-color: #fff;
}

.hostname-setting-modal .ant-form-item-label label {
  color: #1d1d1f;
  font-weight: 500;
}

.hostname-setting-modal .ant-btn-primary {
  background-color: #0071e3;
  border-color: #0071e3;
}

.hostname-setting-modal .ant-btn-primary:hover, 
.hostname-setting-modal .ant-btn-primary:focus {
  background-color: #0077ED;
  border-color: #0077ED;
}

.hostname-setting-modal .ant-select-selection,
.hostname-setting-modal .ant-input,
.hostname-setting-modal .ant-input-number {
  border-radius: 6px;
}

.hostname-setting-modal .ant-select-selection:hover,
.hostname-setting-modal .ant-input:hover,
.hostname-setting-modal .ant-input-number:hover {
  border-color: #0071e3;
}

.hostname-setting-modal .ant-select-focused .ant-select-selection,
.hostname-setting-modal .ant-input:focus,
.hostname-setting-modal .ant-input-number-focused {
  border-color: #0071e3;
  box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
}
</style> 