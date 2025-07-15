<template>
  <div v-if="namespaceConfig.showNamespaceSelector" class="namespace-selector">
    <span class="selector-label">命名空间:</span>
    <a-select 
      v-model="selectedNamespace" 
      style="width: 160px" 
      @change="handleNamespaceChange"
      :loading="loading"
    >
      <a-select-option value="all">所有命名空间</a-select-option>
      <a-select-option 
        v-for="ns in namespaceConfig.namespaces" 
        :key="ns.name" 
        :value="ns.name"
      >
        {{ ns.name }}
      </a-select-option>
    </a-select>
  </div>
</template>

<script>
export default {
  name: 'NamespaceSelector',
  props: {
    clusterId: {
      type: Number,
      required: true
    },
    serviceId: {
      type: [Number, String],
      required: true
    },
    value: {
      type: String,
      default: 'datasophon'
    }
  },
  data() {
    return {
      loading: false,
      selectedNamespace: this.value,
      namespaceConfig: {
        namespaces: [],
        defaultNamespace: 'datasophon',
        showNamespaceSelector: true
      }
    };
  },
  watch: {
    value(newValue) {
      this.selectedNamespace = newValue;
    },
    clusterId() {
      this.fetchNamespaces();
    }
  },
  mounted() {
    this.fetchNamespaces();
  },
  methods: {
    async fetchNamespaces() {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getKubernetesNamespaces, {
          clusterId: this.clusterId
        });
        
        if (res.code === 200 && res.data) {
          this.namespaceConfig = res.data;
          
          // 如果有默认命名空间且当前未选择，使用默认命名空间
          if (this.namespaceConfig.defaultNamespace && !this.selectedNamespace) {
            this.selectedNamespace = this.namespaceConfig.defaultNamespace;
            this.$emit('input', this.selectedNamespace);
            this.$emit('change', this.selectedNamespace);
          }
        } else {
          this.$message.error('获取命名空间列表失败: ' + (res.msg || '未知错误'));
        }
      } catch (error) {
        console.error('获取命名空间列表出错:', error);
        this.$message.error('获取命名空间列表失败: ' + (error.message || '未知错误'));
      } finally {
        this.loading = false;
      }
    },
    handleNamespaceChange(value) {
      this.selectedNamespace = value;
      this.$emit('input', value);
      this.$emit('change', value);
    }
  }
};
</script>

<style lang="less" scoped>
.namespace-selector {
  display: flex;
  align-items: center;
  margin-left: auto; // 靠右对齐
  
  .selector-label {
    margin-right: 8px;
    color: #666;
    white-space: nowrap;
  }
}
</style> 