apiVersion: apps/v1
kind: Deployment
metadata:
  name: "${serviceRoleFullName}"
  namespace: ${namespace}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: "${serviceRoleFullName}"
  template:
    metadata:
      labels:
        app: "${serviceRoleFullName}"
    spec:
      serviceAccountName: flinkoperator-flinkoperator
      containers:
        - name: flink-webhook
          image: "${dockerImage}"  
          imagePullPolicy: IfNotPresent
          command: ["/docker-entrypoint.sh", "webhook"]
          env:
            - name: WEBHOOK_KEYSTORE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: flink-operator-webhook-secret
                  key: password
            - name: WEBHOOK_KEYSTORE_FILE
              value: "/certs/keystore.p12"
            - name: WEBHOOK_KEYSTORE_TYPE
              value: "pkcs12"
            - name: WEBHOOK_SERVER_PORT
              value: "9443"
            - name: LOG_CONFIG
              value: -Dlog4j.configurationFile=/opt/flink/conf/log4j-operator.properties
            - name: FLINK_CONF_DIR
              value: /opt/flink/conf
            - name: FLINK_PLUGINS_DIR
              value: /opt/flink/plugins
            - name: OPERATOR_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
          ports:
            - containerPort: 9443
              name: https
              protocol: TCP
          readinessProbe:
            httpGet:
              path: /readyz
              port: https
              scheme: HTTPS
          livenessProbe:
            httpGet:
              path: /healthz
              port: https
              scheme: HTTPS
          volumeMounts:
            - name: keystore
              mountPath: "/certs"
              readOnly: true
            - name: config-volume
              mountPath: /opt/flink/conf
      nodeSelector:
        ${namespace}-${serviceRoleFullName}: "true"
      volumes:
        - name: keystore
          secret:
            secretName: webhook-server-cert
            items:
              - key: keystore.p12
                path: keystore.p12
        - name: config-volume
          configMap:
            name: flink-operator-config
            items:
              - key: flink-conf.yaml
                path: flink-conf.yaml
              - key: log4j-operator.properties
                path: log4j-operator.properties
              - key: log4j-console.properties
                path: log4j-console.properties

---
apiVersion: v1
kind: Secret
metadata:
  name: flink-operator-webhook-secret
  namespace: ${namespace}
type: Opaque
data:
  password: cGFzc3dvcmQxMjM0

---
# 2. Webhook 服务 (Service)
apiVersion: v1
kind: Service
metadata:
  name: flink-operator-webhook-service
  namespace: ${namespace}
spec:
  ports:
    - name: https
      port: 443
      targetPort: 9443
  selector:
    app: "${serviceRoleFullName}"  

---
# 3. Webhook 证书管理 (Cert-Manager)
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: flink-operator-selfsigned-issuer
  namespace: ${namespace}
spec:
  selfSigned: {}

---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: flink-operator-serving-cert
  namespace: ${namespace}
spec:
  dnsNames:
    - flink-operator-webhook-service.${namespace}.svc
    - flink-operator-webhook-service.${namespace}.svc.cluster.local
  keystores:
    pkcs12:
      create: true
      passwordSecretRef:
        name: flink-operator-webhook-secret
        key: password
  issuerRef:
    kind: Issuer
    name: flink-operator-selfsigned-issuer
  secretName: webhook-server-cert

---
# 4. Webhook 准入控制配置
apiVersion: admissionregistration.k8s.io/v1
kind: MutatingWebhookConfiguration
metadata:
  annotations:
    cert-manager.io/inject-ca-from: ${namespace}/flink-operator-serving-cert
  name: flink-operator-webhook-configuration
webhooks:
  - name: mutationwebhook.flink.apache.org
    admissionReviewVersions: ["v1"]
    clientConfig:
      service:
        name: flink-operator-webhook-service
        namespace: ${namespace}
        path: /mutate
    failurePolicy: Fail
    rules:
      - apiGroups: ["flink.apache.org"]
        apiVersions: ["*"]
        scope: "Namespaced"
        operations:
          - CREATE
          - UPDATE
        resources:
          - flinksessionjobs
          - flinkdeployments
    sideEffects: None

---
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingWebhookConfiguration
metadata:
  annotations:
    cert-manager.io/inject-ca-from: ${namespace}/flink-operator-serving-cert
  name: flink-operator-webhook-configuration
webhooks:
  - name: validationwebhook.flink.apache.org
    admissionReviewVersions: ["v1"]
    clientConfig:
      service:
        name: flink-operator-webhook-service
        namespace: ${namespace}
        path: /validate
    failurePolicy: Fail
    rules:
      - apiGroups: ["flink.apache.org"]
        apiVersions: ["*"]
        scope: "Namespaced"
        operations:
          - CREATE
          - UPDATE
        resources:
          - flinkdeployments
          - flinksessionjobs
          - flinkstatesnapshots
    sideEffects: None
