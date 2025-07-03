# my global config
global:
  scrape_interval:     ${scrape_interval}s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
  evaluation_interval: ${evaluation_interval}s # Evaluate rules every 15 seconds. The default is every 1 minute.
  # scrape_timeout is set to the global default (10s).

# Alertmanager configuration
alerting:
  alertmanagers:
  - file_sd_configs:
    - files:
      - configs/alertmanagers.json

# Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"
  - "alert_rules/*.yml"
# A scrape configuration containing exactly one endpoint to scrape:
# Here it's Prometheus itself.
scrape_configs:
  - job_name: 'k8s-kubelet'
    scheme: https
    tls_config:
      ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
    kubernetes_sd_configs:
    - role: node
    relabel_configs:
    - target_label: __address__
      replacement: kubernetes.default.svc:443
    - source_labels: [__meta_kubernetes_node_name]
      regex: (.+)
      target_label: __metrics_path__
      replacement: /api/v1/nodes/${r"${1}"}/proxy/metrics

  - job_name: 'k8s-cadvisor'
    scheme: https
    tls_config:
      ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
    kubernetes_sd_configs:
    - role: node
    relabel_configs:
    - target_label: __address__
      replacement: kubernetes.default.svc:443
    - source_labels: [__meta_kubernetes_node_name]
      regex: (.+)
      target_label: __metrics_path__
      replacement: /api/v1/nodes/${r"${1}"}/proxy/metrics/cadvisor
    metric_relabel_configs:
    - source_labels: [instance]
      separator: ;
      regex: (.+)
      target_label: node
      replacement: $1
      action: replace

  - job_name: kube-state-metrics
    kubernetes_sd_configs:
    - role: endpoints
      namespaces:
        names:
        - ops-monit
    relabel_configs:
    - source_labels: [__meta_kubernetes_service_label_app_kubernetes_io_name]
      regex: kube-state-metrics
      replacement: $1
      action: keep


  - job_name: 'datasophon-api'
    metrics_path: '/ddh/actuator/prometheus'
    scrape_interval: 5s
    static_configs:
    - targets: ['{{apiUrl}}']

  # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
  - job_name: 'prometheus'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    static_configs:
    - targets: ['localhost:9090']
  - job_name: 'grafana'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    file_sd_configs:
    - files:
      - configs/grafana.json
  - job_name: 'alertmanager'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    file_sd_configs:
    - files:
      - configs/alertmanager.json
  - job_name: 'pushgateway'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    file_sd_configs:
    - files:
      - configs/pushgateway.json


  - job_name: 'node' #自定义名称,用于监控linux基础服务
    file_sd_configs:
     - files:
       - configs/linux.json  #linux机器IP地址json文件
  - job_name: 'namenode'  #用于监控HDFS组件
    file_sd_configs:
     - files:
       - configs/namenode.json  #hdfs参数获取地址
  - job_name: 'datanode'  #用于监控HDFS组件
    file_sd_configs:
     - files:
       - configs/datanode.json  #hdfs参数获取地址
  - job_name: 'resourcemanager' #用于监控Yarn组件
    file_sd_configs:
     - files:
       - configs/resourcemanager.json #yarn参数获取地址
  - job_name: 'zkserver' #用于监控zk组件
    file_sd_configs:
     - files:
       - configs/zkserver.json #zk参数获取地址
  - job_name: 'hiveserver2'
    file_sd_configs:
     - files:
       - configs/hiveserver2.json
  - job_name: 'noahjob'
    file_sd_configs:
     - files:
       - configs/noahjobserver.json
  - job_name: 'spark'
    file_sd_configs:
     - files:
       - configs/spark.json
  - job_name: 'worker'
    file_sd_configs:
     - files:
       - configs/worker.json
  - job_name: 'master'
    file_sd_configs:
     - files:
       - configs/master.json
  - job_name: 'nodemanager'
    file_sd_configs:
     - files:
       - configs/nodemanager.json
  - job_name: 'kafkabroker'
    file_sd_configs:
     - files:
       - configs/kafkabroker.json
  - job_name: 'hbasemaster'
    file_sd_configs:
     - files:
       - configs/hbasemaster.json
  - job_name: 'regionserver'
    file_sd_configs:
     - files:
       - configs/regionserver.json
  - job_name: 'zkfc'
    file_sd_configs:
     - files:
       - configs/zkfc.json
  - job_name: 'journalnode'
    file_sd_configs:
     - files:
       - configs/journalnode.json
  - job_name: 'historyserver'
    file_sd_configs:
     - files:
       - configs/historyserver.json
  - job_name: 'hivemetastore'
    file_sd_configs:
     - files:
       - configs/hivemetastore.json
  - job_name: 'trinocoordinator'
    file_sd_configs:
     - files:
       - configs/trinocoordinator.json
  - job_name: 'trinoworker'
    file_sd_configs:
     - files:
       - configs/trinoworker.json
  - job_name: 'srfe'
    metrics_path: '/metrics'
    relabel_configs:
      - source_labels: []
        target_label: group
        replacement: 'srfe'
    file_sd_configs:
     - files:
       - configs/srfe.json
       - configs/srfeobserver.json
  - job_name: 'srbe'
    metrics_path: '/metrics'
    relabel_configs:
      - source_labels: []
        target_label: group
        replacement: 'srbe'
    file_sd_configs:
     - files:
       - configs/srbe.json
       - configs/srcn.json
  - job_name: 'doris'
    metrics_path: '/metrics'
    file_sd_configs:
     - files:
       - configs/doris.json
  - job_name: 'rangeradmin'
    file_sd_configs:
     - files:
       - configs/rangeradmin.json
  - job_name: 'jobmanager'
    file_sd_configs:
     - files:
       - configs/jobmanager.json
  - job_name: 'taskmanager'
    file_sd_configs:
     - files:
       - configs/taskmanager.json
  - job_name: 'esexporter'
    file_sd_configs:
     - files:
       - configs/esexporter.json
  - job_name: 'apiserver'
    file_sd_configs:
     - files:
       - configs/apiserver.json
  - job_name: 'masterserver'
    file_sd_configs:
     - files:
       - configs/masterserver.json
  - job_name: 'workerserver'
    file_sd_configs:
     - files:
       - configs/workerserver.json
  - job_name: 'alertserver'
    file_sd_configs:
     - files:
       - configs/alertserver.json
  - job_name: 'streampark'
    file_sd_configs:
     - files:
       - configs/streampark.json
  - job_name: 'dinky'
    file_sd_configs:
     - files:
       - configs/dinky.json
  - job_name: 'prestocoordinator'
    file_sd_configs:
     - files:
       - configs/prestocoordinator.json
  - job_name: 'prestoworker'
    file_sd_configs:
     - files:
       - configs/prestoworker.json
  - job_name: 'minio'
    metrics_path: /minio/prometheus/metrics
    scheme: http
    file_sd_configs:
     - files:
       - configs/minioservice.json
  - job_name: 'kyuubi'
    file_sd_configs:
     - files:
       - configs/kyuubiserver.json
  - job_name: 'clickhouse'
    file_sd_configs:
     - files:
       - configs/clickhouse.json
  - job_name: 'alluxiomaster'
    metrics_path: /metrics/prometheus
    file_sd_configs:
     - files:
       - configs/alluxiomaster.json
  - job_name: 'alluxioworker'
    metrics_path: /metrics/prometheus
    file_sd_configs:
     - files:
       - configs/alluxioworker.json
  - job_name: 'redis'
    file_sd_configs:
      - files:
        - configs/redisexporter.json
    metrics_path: /metrics
  - job_name: 'postgres'
    file_sd_configs:
     - files:
       - configs/postgresqlmaster.json
       - configs/postgresqlworker.json
    metrics_path: /metrics
    params:
      auth_module: [foo]
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: 127.0.0.1:9187
  - job_name: 'zeppelin'
    metrics_path: /metrics
    file_sd_configs:
     - files:
       - configs/zeppelinserver.json
  - job_name: 'hue'
    metrics_path: /metrics
    file_sd_configs:
     - files:
       - configs/huemaster.json