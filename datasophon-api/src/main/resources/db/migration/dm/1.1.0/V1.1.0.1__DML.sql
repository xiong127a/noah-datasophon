-- ----------------------------
-- Records of t_ddh_access_token
-- ----------------------------
INSERT INTO t_ddh_access_token VALUES (0, 1, 'test', '2022-06-15 09:51:54', '2022-06-15 09:51:57', '2023-01-01 09:51:59');

-- ----------------------------
-- Records of t_ddh_alert_group
-- ----------------------------
INSERT INTO t_ddh_alert_group VALUES (1, 'HIVE告警组', 'HIVE', '2022-07-14 15:52:45');
INSERT INTO t_ddh_alert_group VALUES (2, 'HDFS告警组', 'HDFS', '2022-07-14 15:52:47');
INSERT INTO t_ddh_alert_group VALUES (3, 'YARN告警组', 'YARN', '2022-07-14 15:52:50');
INSERT INTO t_ddh_alert_group VALUES (8, 'HBASE告警组', 'HBASE', '2022-07-14 15:52:52');
INSERT INTO t_ddh_alert_group VALUES (10, 'KAFKA告警组', 'KAFKA', '2022-07-14 15:52:57');
INSERT INTO t_ddh_alert_group VALUES (11, '主机告警组', 'NODE', '2022-07-14 15:52:59');
INSERT INTO t_ddh_alert_group VALUES (12, 'ZOOKEEPER告警组', 'ZOOKEEPER', '2022-07-14 15:53:02');
INSERT INTO t_ddh_alert_group VALUES (13, 'ALERTMANAGER告警组', 'ALERTMANAGER', '2022-07-14 15:53:05');
INSERT INTO t_ddh_alert_group VALUES (14, 'GRAFANA告警组', 'GRAFANA', '2022-07-14 15:53:07');
INSERT INTO t_ddh_alert_group VALUES (15, 'PROMETHEUS告警组', 'PROMETHEUS', '2022-07-14 15:53:09');
INSERT INTO t_ddh_alert_group VALUES (16, 'SPARK告警组', 'SPARK3', '2022-07-15 14:12:38');
INSERT INTO t_ddh_alert_group VALUES (17, 'TRINO告警组', 'TRINO', '2022-07-24 23:23:01');
INSERT INTO t_ddh_alert_group VALUES (18, 'RANGER告警组', 'RANGER', '2022-09-09 11:29:14');
INSERT INTO t_ddh_alert_group VALUES (19, 'STARROCKS告警组', 'STARROCKS', '2022-09-13 14:53:57');
INSERT INTO t_ddh_alert_group VALUES (20, 'ELASTICSEARCH告警组', 'ELASTICSEARCH', '2022-10-08 16:15:55');
INSERT INTO t_ddh_alert_group VALUES (21, 'DS告警组', 'DS', '2022-11-20 21:00:00');
INSERT INTO t_ddh_alert_group VALUES (22, 'SP告警组', 'STREAMPARK', '2022-11-21 18:20:10');
INSERT INTO t_ddh_alert_group VALUES (23, 'Doris告警组', 'DORIS', '2023-01-07 22:12:36');

-- ----------------------------
-- Records of t_ddh_cluster_service_dashboard  
-- ----------------------------
INSERT INTO t_ddh_cluster_service_dashboard VALUES (1, 'HDFS', 'http://${grafanaHost}:3000/d/huM_B3dZz/2-hdfs?orgId=1&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (2, 'YARN', 'http://${grafanaHost}:3000/d/-ZErfqOWz/3-yarn?orgId=1&refresh=30s&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (3, 'HIVE', 'http://${grafanaHost}:3000/d/WYNeBqdZz/5-hive?orgId=1&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (4, 'HBASE', 'http://${grafanaHost}:3000/d/_S8XBqOWz/4-hbase?orgId=1&refresh=30s&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (5, 'KAFKA', 'http://${grafanaHost}:3000/d/DGHHkJKWk/6-kafka?orgId=1&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (6, 'ZOOKEEPER', 'http://${grafanaHost}:3000/d/000000261/8-zookeeper?orgId=1&refresh=1m&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (7, 'RANGER', 'http://${grafanaHost}:3000/d/qgVDEd3nk/ranger?orgId=1&refresh=30s&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (8, 'PROMETHEUS', 'http://${grafanaHost}:3000/d/dd4t3A6nz/prometheus-2-0-overview?orgId=1&refresh=30s&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (9, 'GRAFANA', 'http://${grafanaHost}:3000/d/eea-11_sik/grafana?orgId=1&refresh=5m&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (10, 'ALERTMANAGER', 'http://${grafanaHost}:3000/d/eea-9_siks/alertmanager?orgId=1&refresh=5m&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (11, 'SPARK3', 'http://${grafanaHost}:3000/d/rCUqf3dWz/7-spark?orgId=1&from=now-30m&to=now&refresh=5m&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (12, 'TOTAL', 'http://${grafanaHost}:3000/d/_4gf-qOZz/1-zong-lan?orgId=1&refresh=30s&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (13, 'TRINO', 'http://${grafanaHost}:3000/d/TGzKne5Wk/trino?orgId=1&refresh=30s&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (14, 'STARROCKS', 'http://${grafanaHost}:3000/d/wpcA3tG7z/starrocks?orgId=1&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (15, 'FLINK', 'http://${grafanaHost}:3000/d/-0rFuzoZk/flink-dashboard?orgId=1&refresh=30s&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (16, 'ELASTICSEARCH', 'http://${grafanaHost}:3000/d/3788af4adc3046dd92b3af31d0150c79/elasticsearch-cluster?orgId=1&refresh=5m&var-cluster=ddp_es&var-name=All&var-interval=5m&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (17, 'DS', 'http://${grafanaHost}:3000/d/X_NPpJOVk/dolphinscheduler?refresh=1m&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (18, 'STREAMPARK', 'http://${grafanaHost}:3000/d/98U0T1OVz/streampark?kiosk&refresh=1m');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (19, 'DINKY', 'http://${grafanaHost}:3000/d/9qU9T1OVk/dinky?kiosk&refresh=1m');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (20, 'DORIS', 'http://${grafanaHost}:3000/d/1fFiWJ4mz/doris-overview?orgId=1&from=now-6h&to=now&refresh=1m&kiosk');
INSERT INTO t_ddh_cluster_service_dashboard VALUES (21, 'KERBEROS', 'http://${grafanaHost}:3000/d/QflaxlA4k/kerberos?orgId=1&refresh=1m&kiosk');

-- ----------------------------
-- Records of t_ddh_install_step
-- ----------------------------
INSERT INTO t_ddh_install_step VALUES (1, '安装主机', NULL, 1);
INSERT INTO t_ddh_install_step VALUES (2, '主机环境校验', NULL, 1);
INSERT INTO t_ddh_install_step VALUES (3, '分发安装启动主机agent', NULL, 1);
INSERT INTO t_ddh_install_step VALUES (4, '选择服务', NULL, 1);
INSERT INTO t_ddh_install_step VALUES (5, '分配服务Master角色', NULL, 1);
INSERT INTO t_ddh_install_step VALUES (6, '分配服务Worker与Client角色', NULL, 1);
INSERT INTO t_ddh_install_step VALUES (7, '服务配置', NULL, 1);
INSERT INTO t_ddh_install_step VALUES (8, '服务安装总览', NULL, 1);
INSERT INTO t_ddh_install_step VALUES (9, '服务安装启动', NULL, 1);

-- ----------------------------
-- Records of t_ddh_cluster_group
-- ----------------------------
INSERT INTO t_ddh_cluster_group VALUES (1, 'hadoop', 1);
INSERT INTO t_ddh_cluster_group VALUES (2, 'elastic', 1);
INSERT INTO t_ddh_cluster_group VALUES (3, 'root', 1);
INSERT INTO t_ddh_cluster_group VALUES (4, 'hue', 1);
INSERT INTO t_ddh_cluster_group VALUES (5, 'postgres', 1);

-- ----------------------------
-- Records of t_ddh_cluster_user
-- ----------------------------
INSERT INTO t_ddh_cluster_user VALUES (1, 'hdfs', 1);
INSERT INTO t_ddh_cluster_user VALUES (2, 'hive', 1);
INSERT INTO t_ddh_cluster_user VALUES (3, 'yarn', 1);
INSERT INTO t_ddh_cluster_user VALUES (4, 'mapred', 1);
INSERT INTO t_ddh_cluster_user VALUES (5, 'elastic', 1);
INSERT INTO t_ddh_cluster_user VALUES (6, 'hbase', 1);
INSERT INTO t_ddh_cluster_user VALUES (7, 'hue', 1);
INSERT INTO t_ddh_cluster_user VALUES (8, 'postgres', 1);
INSERT INTO t_ddh_cluster_user VALUES (9, 'admin', 1);
INSERT INTO t_ddh_cluster_user VALUES (10, 'root', 1);

-- ----------------------------
-- Records of t_ddh_cluster_user_group
-- ----------------------------
INSERT INTO t_ddh_cluster_user_group VALUES (1, 1, 1, 1, 1);
INSERT INTO t_ddh_cluster_user_group VALUES (2, 2, 1, 1, 1);
INSERT INTO t_ddh_cluster_user_group VALUES (3, 3, 1, 1, 1);
INSERT INTO t_ddh_cluster_user_group VALUES (4, 4, 1, 1, 1);
INSERT INTO t_ddh_cluster_user_group VALUES (5, 5, 2, 1, 1);
INSERT INTO t_ddh_cluster_user_group VALUES (6, 6, 1, 1, 1);
INSERT INTO t_ddh_cluster_user_group VALUES (7, 7, 4, 1, 1);
INSERT INTO t_ddh_cluster_user_group VALUES (8, 8, 5, 1, 1);
INSERT INTO t_ddh_cluster_user_group VALUES (9, 9, 1, 1, 1);
INSERT INTO t_ddh_cluster_user_group VALUES (10, 10, 3, 1, 1);

-- ----------------------------
-- Records of t_ddh_session
-- ----------------------------
INSERT INTO t_ddh_session VALUES ('3f229c41-84ee-4a09-a0b9-76e95f0577dc', 2, '192.168.75.12', '2022-09-07 11:52:12');
INSERT INTO t_ddh_session VALUES ('d25dd005-ceb6-4414-bfdf-9279a23c2ba6', 1, '192.168.75.12', '2023-02-12 20:34:57');

-- ----------------------------
-- Records of t_ddh_user_info
-- ----------------------------
INSERT INTO t_ddh_user_info VALUES (1, 'admin', '$2a$12$CxjcBsUr5xC1SoJ9J6tuuO9e7AYYz3sHKXnCKBR1ZdL94Y6ZRy9tu', 'admin@datasophon.com', '18600000000', CURRENT_TIMESTAMP, 1);
