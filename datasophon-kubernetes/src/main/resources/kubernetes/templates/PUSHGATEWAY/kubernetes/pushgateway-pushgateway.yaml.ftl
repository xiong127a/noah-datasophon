apiVersion: "apps/v1"
kind: "StatefulSet"
metadata:
labels:
name: "${serviceRoleFullName}"
name: "${serviceRoleFullName}"
namespace: ${namespace}
spec:
serviceName: "${serviceRoleFullName}"
replicas: ${roleNodeCnt}
selector:
matchLabels:
app: "${serviceRoleFullName}"
minReadySeconds: 5
revisionHistoryLimit: 10
template:
metadata:
labels:
name: "${serviceRoleFullName}"
app: "${serviceRoleFullName}"
podConflictName: "${serviceRoleFullName}"
annotations:
serviceInstanceName: "${serviceName}"
service.kubernetes.io/headless: "true"
spec:
nodeSelector:
${serviceRoleFullName}: "true"
affinity:
podAntiAffinity:
requiredDuringSchedulingIgnoredDuringExecution:
- labelSelector:
matchLabels:
name: "${serviceRoleFullName}"
podConflictName: "${serviceRoleFullName}"
namespaces:
- "${namespace}"
topologyKey: "kubernetes.io/hostname"
hostPID: false
hostNetwork: false
initContainers:
- name: set-permissions
image: "${dockerBusyboxImage}"
env:
- name: POD_NAME
valueFrom:
fieldRef:
fieldPath: metadata.name
- name: POD_NAMESPACE
valueFrom:
fieldRef:
fieldPath: metadata.namespace
command:
- "/bin/sh"
- "-c"
- |
echo "========== 开始准备Prometheus数据目录和权限 =========="

echo "Setting permissions for Prometheus PVC mount path..."
chmod -R 777 ${mount_path}
echo "Permissions set successfully"

echo "========== 完成数据目录和权限设置 =========="
securityContext:
runAsUser: 0  # 以root用户运行
privileged: true
volumeMounts:
- name: prometheus-data
mountPath: ${mount_path}
subPathExpr: $(POD_NAME)
containers:
- env:
- name: USER
value: ${runAsUser}
- name: MEM_LIMIT
valueFrom:
resourceFieldRef:
resource: limits.memory
- name: POD_NAME
valueFrom:
fieldRef:
fieldPath: metadata.name
- name: POD_NAMESPACE
valueFrom:
fieldRef:
fieldPath: metadata.namespace
image: "${dockerImage}"
imagePullPolicy: "Always"
command:
- "/bin/bash"
- "-c"
- |
ulimit -n 1000000
ulimit -u 65535
sysctl -w fs.file-max=1000000
sysctl -w fs.inotify.max_user_watches=524288
sysctl -w fs.inotify.max_user_instances=524288
${startCommand}
readinessProbe:
exec:
command:
- "/bin/bash"
- "-c"
- "${statusCommand}"
failureThreshold: 3
initialDelaySeconds: 3
periodSeconds: 30
successThreshold: 1
timeoutSeconds: 15
name: "${serviceRoleFullName}"
resources:
requests:
memory: ${requests_memory}
cpu: ${requests_cpu}
limits:
memory: ${limits_memory}
cpu: ${limits_cpu}
securityContext:
privileged: true
capabilities:
add: ["SYS_RESOURCE"]
runAsUser: 0
runAsGroup: 0
fsGroup: 0
runAsNonRoot: false
allowPrivilegeEscalation: true
readOnlyRootFilesystem: false
seLinuxOptions:
level: "s0:c123,c456"
windowsOptions:
runAsUserName: "ContainerAdministrator"
sysctls:
- name: fs.file-max
value: "1000000"
- name: fs.inotify.max_user_watches
value: "524288"
- name: fs.inotify.max_user_instances
value: "524288"
volumeMounts:
- name: prometheus-data
mountPath: ${mount_path}
subPathExpr: $(POD_NAME)
<#list volumeConfigMapSet as item>
  - name: "${item.name}"
  mountPath: "${item.value}"
  subPath: "${item.fileName}"
</#list>
- name: "timezone"
mountPath: "/etc/localtime"
volumes:
- name: prometheus-data
persistentVolumeClaim:
claimName: "${serviceRoleFullName}"
<#list volumeConfigMapSet as item>
  - name: "${item.name}"
  configMap:
  name: "${item.name}"
</#list>
- name: "timezone"
hostPath:
path: "/etc/localtime"