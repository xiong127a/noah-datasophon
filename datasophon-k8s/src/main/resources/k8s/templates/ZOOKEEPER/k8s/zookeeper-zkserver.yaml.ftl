apiVersion: apps/v1
kind: StatefulSet
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
  volumeClaimTemplates:
    - metadata:
        name: nfs-pvc
      spec:
        accessModes: [ "ReadWriteOnce" ]
        storageClassName: <#if zkserver_storage_classes??>${zkserver_storage_classes}<#else>standard</#if>
        resources:
          requests:
            storage: <#if zkserver_storage??>${zkserver_storage}<#else>10Gi</#if>
  minReadySeconds: 5
  revisionHistoryLimit: 10
  podManagementPolicy: Parallel
  template:
    metadata:
      labels:
        name: "${serviceRoleFullName}"
        app: "${serviceRoleFullName}"
        podConflictName: "${serviceRoleFullName}"
      annotations:
        serviceInstanceName: "${serviceName}"
    spec:
      initContainers:
        - name: init-myid
          image: ${dockerBusyboxImage}
          env:
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: ZOO_DATA_DIR
              value: ${dataDir}
          command:
            - /bin/sh
            - -c
            - |-
              mkdir -p ${dataDir}
              MY_ID=${r"${HOSTNAME##*-}"}
              echo $((MY_ID + 1)) > ${dataDir}/myid
          volumeMounts:
            - name: nfs-pvc
              mountPath: <#if zkserver_mount_path??>${zkserver_mount_path}<#else>/data</#if>
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchLabels:
                  name: "${serviceRoleFullName}"
                  podConflictName: "${serviceRoleFullName}"
              namespaces:
                - "${namespace}"
              topologyKey: kubernetes.io/hostname
      containers:
        - env:
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: ZOOCFGDIR
              value: "/opt/datasophon/zookeeper-3.5.10/conf"
            - name: ZOO_DATA_DIR
              value: ${dataDir}
            - name: USER
              value: ${runAs}
            - name: MEM_LIMIT
              valueFrom:
                resourceFieldRef:
                  resource: limits.memory
          image: "${dockerImage}"
          ports:
          <#if zkserver_node_port_mappings??>
          <#assign mappings = zkserver_node_port_mappings>
          <#list mappings as item>
            - containerPort: ${(item?keys[0])}
              name: nodeport-${item?index + 1}
          </#list>
          </#if>
          <#if zkserver_cluster_port_mappings??>
          <#assign mappings = zkserver_cluster_port_mappings>
          <#list mappings as item>
            - containerPort: ${(item?keys[0])}
              name: clusterport-${item?index + 1}
          </#list>
          </#if>
          imagePullPolicy: Always
          command:
            - "/bin/bash"
            - "-c"
            - ${startCommand}
          readinessProbe:
            tcpSocket:
              port: 2181
            failureThreshold: 3
            initialDelaySeconds: 3
            periodSeconds: 30
            successThreshold: 1
            timeoutSeconds: 15
          name: "${serviceRoleFullName}"
          resources:
            requests:
              memory: <#if zkserver_requests_memory??>${zkserver_requests_memory}<#else>512Mi</#if>
              cpu: <#if zkserver_requests_cpu??>${zkserver_requests_cpu}<#else>0.5</#if>
            limits:
              memory: <#if zkserver_limits_memory??>${zkserver_limits_memory}<#else>1024Mi</#if>
              cpu: <#if zkserver_limits_cpu??>${zkserver_limits_cpu}<#else>1</#if>
          securityContext:
            privileged: true
          volumeMounts:
            - name: nfs-pvc
              mountPath: <#if zkserver_mount_path??>${zkserver_mount_path}<#else>/data</#if>
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
      nodeSelector:
        ${serviceRoleFullName}: "true"
      terminationGracePeriodSeconds: 30
      volumes:
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"