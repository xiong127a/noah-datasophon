<template>
  <span :class="statusClass" class="status-indicator"></span>
</template>

<script>
// 定义常量在组件定义之外，这样HMR不会替换它们
const VALID_RESOURCE_TYPES = [
  'deployment', 
  'pod', 
  'persistentvolume', 
  'persistentvolumeclaim', 
  'replicaset', 
  'service', 
  'daemonset', 
  'job', 
  'ingress',
  'statefulset'
];

// 检测是否为开发模式
const isDevelopment = process.env.NODE_ENV === 'development';

export default {
  name: 'StatusIndicator',
  props: {
    /**
     * 资源对象，可以是deployment、pod、persistentVolume等
     */
    resource: {
      type: Object,
      required: true
    },
    /**
     * 资源类型，用于确定状态判断逻辑
     * 可选值: 'deployment', 'pod', 'persistentvolume', 'persistentvolumeclaim', 'replicaset', 'service', 'daemonset', 'job', 'ingress'.
     */
    resourceType: {
      type: String,
      required: true,
      validator: function(value) {
        // 开发模式下更宽松的验证，避免HMR问题
        if (isDevelopment) {
          // 检查是否在热更新过程中
          const isHotUpdate = new Error().stack?.includes('.hot-update.js');
          
          // 在热更新过程中，对ingress类型直接返回true
          if (isHotUpdate && String(value).toLowerCase() === 'ingress') {
            console.debug('StatusIndicator: HMR detected, bypassing validation for ingress');
            return true;
          }
        }

        // 检查值是否为null或undefined或空字符串
        if (value === null || value === undefined || value === '') {
          console.error('StatusIndicator: resourceType prop cannot be null, undefined or empty');
          return false;
        }
        
        // 将值转换为小写进行比较
        try {
          const lowerValue = String(value).toLowerCase();
          
          // 使用预定义的常量数组进行验证
          const isValid = VALID_RESOURCE_TYPES.includes(lowerValue);
          
          if (!isValid) {
            // 记录更详细的错误以便调试
            console.error(`StatusIndicator: Invalid resourceType "${value}" (${typeof value}). Valid types are: ${VALID_RESOURCE_TYPES.join(', ')}`);
          }
          
          return isValid;
        } catch (e) {
          console.error('StatusIndicator: Error validating resourceType:', e);
          return false;
        }
      }
    }
  },
  data() {
    return {
      normalizedResourceType: ''
    };
  },
  created() {
    // 规范化resourceType
    this.normalizeResourceType();
  },
  watch: {
    resourceType: {
      immediate: true,
      handler() {
        this.normalizeResourceType();
      }
    }
  },
  computed: {
    statusClass() {
      const classNames = ['status-dot'];
      
      // 使用规范化的resourceType
      const resourceType = this.normalizedResourceType;
      
      // 如果resourceType无效，返回unknown状态
      if (!resourceType) {
        return classNames.concat(['status-unknown']).join(' ');
      }
      
      try {
        switch(resourceType) {
          case 'deployment':
            return this.getDeploymentStatusClass(this.resource);
          case 'pod':
            return this.getPodStatusClass(this.resource);
          case 'persistentvolume':
            return this.getPersistentVolumeStatusClass(this.resource);
          case 'persistentvolumeclaim':
            return this.getPersistentVolumeClaimStatusClass(this.resource);
          case 'replicaset':
            return this.getReplicaSetStatusClass(this.resource);
          case 'service':
            return this.getServiceStatusClass(this.resource);
          case 'daemonset':
            return this.getDaemonSetStatusClass(this.resource);
          case 'job':
            return this.getJobStatusClass(this.resource);
          case 'ingress':
            return this.getIngressStatusClass(this.resource);
          case 'statefulset':
            return this.getStatefulSetStatusClass(this.resource);
          default:
            return classNames.concat(['status-unknown']).join(' ');
        }
      } catch (e) {
        console.error('StatusIndicator: Error determining status class:', e);
        return classNames.concat(['status-error']).join(' ');
      }
    }
  },
  methods: {
    normalizeResourceType() {
      if (this.resourceType) {
        this.normalizedResourceType = String(this.resourceType).toLowerCase();
      } 
      // 尝试从resource的typeMeta中获取kind作为备用
      else if (this.resource && this.resource.typeMeta && this.resource.typeMeta.kind) {
        this.normalizedResourceType = String(this.resource.typeMeta.kind).toLowerCase();
        
        if (isDevelopment) {
          console.debug(`StatusIndicator auto-detected resourceType from typeMeta: "${this.normalizedResourceType}"`);
        }
      }
      
      // 开发模式下记录详细信息
      if (isDevelopment) {
        console.debug(`StatusIndicator resourceType: "${this.resourceType}" (normalized: "${this.normalizedResourceType}")`);
      }
    },
    getDeploymentStatusClass(resource) {
      const classNames = ['status-dot'];
      if (resource?.pods?.running > 0) classNames.push('status-running');
      if (resource?.pods?.pending > 0) classNames.push('status-warning');
      if (resource?.pods?.failed > 0) classNames.push('status-danger');
      if (!resource?.pods || (!resource?.pods.running && !resource?.pods.pending && !resource?.pods.failed))
        classNames.push('status-unknown');
      return classNames.join(' ');
    },
    
    getPodStatusClass(resource) {
      const classNames = ['status-dot'];
      if (!resource || !resource.status) {
        return classNames.concat(['status-unknown']).join(' ');
      }
      
      const status = resource.status.toLowerCase();
      if (status.includes('running')) {
        classNames.push('status-running');
      } else if (status.includes('pending') || status.includes('waiting')) {
        classNames.push('status-warning');
      } else if (status.includes('error') || status.includes('failed') || status.includes('crashloopbackoff')) {
        classNames.push('status-danger');
      } else if (status.includes('completed') || status.includes('succeeded')) {
        classNames.push('succeeded');
      } else if (status.includes('terminating')) {
        classNames.push('terminating');
      } else {
        classNames.push('status-unknown');
      }
      
      return classNames.join(' ');
    },
    
    getPersistentVolumeStatusClass(resource) {
      const classNames = ['status-dot'];
      if (!resource || !resource.status) {
        return classNames.concat(['status-unknown']).join(' ');
      }
      
      const status = resource.status.toLowerCase();
      if (status === 'bound') {
        classNames.push('status-running');
      } else if (status === 'available') {
        classNames.push('status-warning');
      } else if (status === 'released') {
        classNames.push('status-info');
      } else if (status === 'failed') {
        classNames.push('status-danger');
      } else {
        classNames.push('status-unknown');
      }
      
      return classNames.join(' ');
    },
    
    getPersistentVolumeClaimStatusClass(resource) {
      const classNames = ['status-dot'];
      if (!resource || !resource.status) {
        return classNames.concat(['status-unknown']).join(' ');
      }
      
      const status = resource.status.toLowerCase();
      if (status === 'bound') {
        classNames.push('status-running');
      } else if (status === 'pending') {
        classNames.push('status-warning');
      } else if (status === 'lost') {
        classNames.push('status-danger');
      } else {
        classNames.push('status-unknown');
      }
      
      return classNames.join(' ');
    },
    
    getReplicaSetStatusClass(resource) {
      const classNames = ['status-dot'];
      if (resource?.podInfo?.running > 0) classNames.push('status-running');
      if (resource?.podInfo?.pending > 0) classNames.push('status-warning');
      if (resource?.podInfo?.failed > 0) classNames.push('status-danger');
      if (!resource?.podInfo || (!resource?.podInfo.running && !resource?.podInfo.pending && !resource?.podInfo.failed))
        classNames.push('status-unknown');
      return classNames.join(' ');
    },
    
    getServiceStatusClass(resource) {
      const classNames = ['status-dot'];
      // 服务通常只要存在就是可用的，所以默认为running状态
      classNames.push('status-running');
      return classNames.join(' ');
    },
    
    getDaemonSetStatusClass(resource) {
      const classNames = ['status-dot'];
      // DaemonSet状态逻辑与ReplicaSet类似，基于pod信息
      if (resource?.podInfo?.running > 0) classNames.push('status-running');
      if (resource?.podInfo?.pending > 0) classNames.push('status-warning');
      if (resource?.podInfo?.failed > 0) classNames.push('status-danger');
      if (!resource?.podInfo || (!resource?.podInfo.running && !resource?.podInfo.pending && !resource?.podInfo.failed))
        classNames.push('status-unknown');
      return classNames.join(' ');
    },
    
    getJobStatusClass(resource) {
      const classNames = ['status-dot'];
      if (!resource || !resource.jobStatus) {
        return classNames.concat(['status-unknown']).join(' ');
      }
      
      const status = resource.jobStatus.status?.toLowerCase() || '';
      
      if (status === 'complete') {
        classNames.push('status-running'); // 使用运行中的状态样式表示完成
      } else if (status === 'running') {
        classNames.push('status-warning'); // 使用警告状态样式表示正在运行
      } else if (status === 'failed') {
        classNames.push('status-danger'); // 使用危险状态样式表示失败
      } else {
        classNames.push('status-unknown'); // 其他状态视为未知
      }
      
      // 考虑Pod的状态
      if (resource.podInfo) {
        if (resource.podInfo.succeeded > 0) classNames.push('status-running');
        if (resource.podInfo.pending > 0) classNames.push('status-warning');
        if (resource.podInfo.failed > 0) classNames.push('status-danger');
      }
      
      return classNames.join(' ');
    },
    
    getIngressStatusClass(resource) {
      const classNames = ['status-dot'];
      
      // Ingress通常只要存在就是正常状态
      // 但我们可以根据endpoints是否存在判断更精确的状态
      if (!resource || !resource.endpoints || resource.endpoints.length === 0) {
        classNames.push('status-warning'); // 没有endpoints时显示警告状态
      } else {
        classNames.push('status-running'); // 有endpoints时显示正常运行状态
      }
      
      return classNames.join(' ');
    },
    getStatefulSetStatusClass(resource) {
      const classNames = ['status-dot'];
      // StatefulSet状态逻辑与ReplicaSet/Deployment类似，基于pod信息
      if (resource?.podInfo?.running > 0) classNames.push('status-running');
      if (resource?.podInfo?.pending > 0) classNames.push('status-warning');
      if (resource?.podInfo?.failed > 0) classNames.push('status-danger');
      if (!resource?.podInfo || (!resource?.podInfo.running && !resource?.podInfo.pending && !resource?.podInfo.failed))
        classNames.push('status-unknown');
      
      // 检查是否所有期望的pod都在运行
      if (resource?.podInfo?.current < resource?.podInfo?.desired) {
        classNames.push('status-warning'); // 如果当前pod数量小于期望值，显示警告状态
      }
      
      // 检查是否有警告
      if (resource?.podInfo?.warnings && resource.podInfo.warnings.length > 0) {
        classNames.push('status-warning');
      }
      
      return classNames.join(' ');
    }
  }
};
</script>

<style lang="less" scoped>
/* 组件使用公共样式，不需要在这里定义重复的样式 */
</style> 