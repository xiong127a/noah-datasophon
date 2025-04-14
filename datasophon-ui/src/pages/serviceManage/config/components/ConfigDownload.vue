<template>
  <div class="config-download-container">
    <div class="download-header">
      <a-button type="primary" :loading="downloadLoading" @click="downloadAllConfigs">
        <a-icon type="download" />
        打包下载所有配置
      </a-button>
    </div>
    
    <a-table
      :columns="columns"
      :data-source="configFiles"
      :loading="loading"
      :pagination="false"
      :scroll="{ y: 600 }"
      rowKey="fileName"
    >
      <template slot="operation" slot-scope="text, record">
        <a-button type="link" @click="downloadSingleConfig(record)">
          <a-icon type="download" />
          下载
        </a-button>
        <a-button type="link" @click="previewConfig(record)">
          <a-icon type="eye" />
          预览
        </a-button>
      </template>
    </a-table>

    <a-modal
      v-model="previewVisible"
      title="配置文件预览"
      width="70%"
      :footer="null"
    >
      <a-spin :spinning="previewLoading">
        <pre class="config-preview">{{ previewContent }}</pre>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
export default {
  name: 'ConfigDownload',
  props: {
    serviceId: {
      type: [Number, String],
      required: true
    },
    serviceName: {
      type: String,
      required: true,
      default: '未知服务'
    }
  },
  data() {
    return {
      configFiles: [],
      loading: false,
      downloadLoading: false,
      previewVisible: false,
      previewLoading: false,
      previewContent: '',
      columns: [
        {
          title: '配置文件名称',
          dataIndex: 'fileName',
          key: 'fileName',
          width: '30%'
        },
        {
          title: '描述',
          dataIndex: 'description',
          key: 'description',
          width: '40%'
        },
        {
          title: '大小',
          dataIndex: 'fileSize',
          key: 'fileSize',
          width: '15%'
        },
        {
          title: '操作',
          key: 'operation',
          dataIndex: 'operation',
          width: '15%',
          scopedSlots: { customRender: 'operation' }
        }
      ]
    };
  },
  mounted() {
    console.log('ConfigDownload组件已挂载，serviceId:', this.serviceId);
    console.log('全局API对象:', global.API);
    
    // 添加一个测试按钮
    window.testConfigAPI = () => {
      console.log('手动测试API调用');
      this.fetchConfigFiles();
    };
    
    this.fetchConfigFiles();
  },
  methods: {
    // 获取配置文件列表
    async fetchConfigFiles() {
      console.log('开始获取配置文件列表，serviceId:', this.serviceId);
      if (!this.serviceId) {
        console.error('serviceId为空，无法获取配置文件列表');
        this.$message.error('服务ID不能为空');
        return;
      }
      
      this.loading = true;
      try {
        const apiUrl = global.API.getServiceConfigFiles;
        console.log('调用API:', apiUrl);
        
        const params = { serviceInstanceId: this.serviceId };
        console.log('请求参数:', params);
        
        // 检查全局API对象
        if (!global.API || !global.API.getServiceConfigFiles) {
          console.error('全局API对象未正确配置');
          this.$message.error('系统配置错误，无法获取服务信息');
          return;
        }
        
        const res = await this.$axiosJsonPost(apiUrl, params);
        console.log('获取配置文件响应:', res);
        
        if (res.code === 200) {
          this.configFiles = res.data || [];
          console.log('成功获取配置文件列表，数量:', this.configFiles.length);
        } else {
          console.error('获取配置文件列表返回错误:', res.msg);
          this.$message.error(res.msg || '获取配置文件列表失败');
        }
      } catch (error) {
        console.error('获取配置文件列表异常:', error);
        this.$message.error(`获取配置文件列表失败: ${error ? error.message || '未知错误' : '未知错误'}`);
      } finally {
        this.loading = false;
      }
    },

    // 下载单个配置文件
    downloadSingleConfig(record) {
      const { fileName } = record;
      
      // 检查API是否配置
      if (!global.API || !global.API.downloadServiceConfigFile) {
        console.error('全局API对象未正确配置');
        this.$message.error('系统配置错误，无法下载文件');
        return;
      }
      
      try {
        // 构建下载URL
        const downloadUrl = `${window.location.origin}${global.API.downloadServiceConfigFile}?serviceInstanceId=${this.serviceId}&fileName=${encodeURIComponent(fileName)}`;
        console.log('下载单个文件URL:', downloadUrl);
        
        // 创建隐藏的a标签并触发下载
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      } catch (error) {
        console.error('下载文件失败:', error);
        this.$message.error('下载文件失败');
      }
    },

    // 打包下载所有配置文件
    async downloadAllConfigs() {
      this.downloadLoading = true;
      try {
        // 检查API是否配置
        if (!global.API || !global.API.downloadAllServiceConfigFiles) {
          console.error('全局API对象未正确配置');
          this.$message.error('系统配置错误，无法下载文件');
          return;
        }
        
        // 构建下载URL
        const downloadUrl = `${window.location.origin}${global.API.downloadAllServiceConfigFiles}?serviceInstanceId=${this.serviceId}`;
        console.log('打包下载URL:', downloadUrl);
        
        // 创建隐藏的a标签并触发下载
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.setAttribute('download', `${this.serviceName}_configs.zip`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      } catch (error) {
        console.error('下载配置文件失败:', error);
        this.$message.error('下载配置文件失败');
      } finally {
        this.downloadLoading = false;
      }
    },

    // 预览配置文件
    async previewConfig(record) {
      this.previewVisible = true;
      this.previewLoading = true;
      this.previewContent = '';

      try {
        console.log('预览文件:', record.fileName);
        
        // 检查API是否配置
        if (!global.API || !global.API.previewServiceConfigFile) {
          console.error('全局API对象未正确配置');
          this.previewContent = '系统配置错误，无法获取文件内容';
          return;
        }
        
        const params = {
          serviceInstanceId: this.serviceId,
          fileName: record.fileName
        };
        console.log('预览请求参数:', params);
        console.log('预览API路径:', global.API.previewServiceConfigFile);
        
        const res = await this.$axiosJsonPost(global.API.previewServiceConfigFile, params);
        console.log('预览文件响应:', res);
        
        if (res.code === 200) {
          this.previewContent = res.data || '文件内容为空';
          console.log('预览成功');
        } else {
          console.error('预览失败:', res.msg);
          this.previewContent = '获取文件内容失败: ' + (res.msg || '未知错误');
        }
      } catch (error) {
        console.error('预览文件异常:', error);
        this.previewContent = '获取文件内容失败: ' + (error && error.message ? error.message : '未知错误');
      } finally {
        this.previewLoading = false;
      }
    }
  }
};
</script>

<style scoped>
.config-download-container {
  padding: 24px;
}

.download-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.config-preview {
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 600px;
  overflow-y: auto;
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 14px;
}
</style> 