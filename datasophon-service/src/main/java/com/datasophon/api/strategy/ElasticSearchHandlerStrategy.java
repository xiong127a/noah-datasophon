/*
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
 */

package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.CommandLineItem;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datasophon.api.utils.ProcessUtils.getDepMode;
import static com.datasophon.common.utils.HostUtils.generateHosts;

public class ElasticSearchHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        String depMode = getDepMode(clusterId);
        if (!Constants.PVM_MODE.equals(depMode)) {
            hosts = generateHosts(hosts, "elasticsearch-elasticsearch");
        }
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${initMasterNodes}", String.join(",", hosts));
        String seedHosts = hosts.stream()
                .map(host -> host + ":9300")
                .collect(Collectors.joining(","));
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${seedHosts}", seedHosts);
        if (CollUtil.isNotEmpty(hosts)) {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${esSingleHost}", hosts.get(0));
        }

    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        for (ServiceConfig config : list) {
            if ("http.port".equals(config.getName())) {
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${esHttpPort}", Convert.toStr(config.getValue()));
            }
        }
    }

    /**
     * 获取ElasticSearch连接信息
     *
     * @param clusterId         集群ID
     * @param serviceInstanceId 服务实例ID
     * @return 连接信息
     */
    @Override
    public ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId) {
        try {
            // 1. 获取服务配置
            Pair<String, List<ServiceConfig>> pair = listServiceConfigByServiceInstance(serviceInstanceId);
            List<ServiceConfig> serviceConfigs = pair.getValue();
            String esHome = pair.getKey();

            // 2. 从配置中解析配置到map，方便快速查询
            Map<String, String> configMap = new HashMap<>();
            for (ServiceConfig config : serviceConfigs) {
                if (config.getValue() != null) {
                    configMap.put(config.getName(), String.valueOf(config.getValue()));
                }
            }

            // 3. 获取ES节点列表 - 只使用ElasticSearch角色，不区分Master/Data/Coordinating
            List<String> esNodes = getRoleHosts(clusterId, serviceInstanceId, "ElasticSearch");

            // 如果没有找到ES节点，返回空信息
            if (CollUtil.isEmpty(esNodes)) {
                log.warn("未找到ElasticSearch节点，集群ID: {}", clusterId);
                return ConnectionInfo.builder().build();
            }

            // 4. 获取端口配置
            String httpPort = configMap.getOrDefault("http.port", "9200");
            String transportPort = configMap.getOrDefault("transport.port", "9300");
            String clusterName = configMap.getOrDefault("cluster.name", "datasophon-es");

            // 5. 判断是否启用了安全认证
            boolean enableSecurity = "true".equalsIgnoreCase(configMap.getOrDefault("xpack.security.enabled", "false"));
            String securityUser = configMap.getOrDefault("elastic.username", "elastic");
            String securityPassword = configMap.getOrDefault("elastic.password", "");

            // 6. 构建HTTP和Transport连接地址
            // 构建HTTP连接地址
            String httpAddresses = esNodes.stream()
                    .map(node -> node + ":" + httpPort)
                    .collect(Collectors.joining(","));

            // 构建Transport连接地址
            String transportAddresses = esNodes.stream()
                    .map(node -> node + ":" + transportPort)
                    .collect(Collectors.joining(","));

            // 7. 构建基本连接信息
            Map<String, String> basicInfo = new HashMap<>();
            basicInfo.put("集群名称", clusterName);
            basicInfo.put("HTTP接口", httpAddresses);
            basicInfo.put("Transport接口", transportAddresses);
            basicInfo.put("节点列表", StrUtil.join(",", esNodes));
            basicInfo.put("安全认证", enableSecurity ? "是" : "否");

            // 8. 构建有序的基本连接信息列表（用于前端表格显示）
            List<Map<String, String>> basicInfoList = new ArrayList<>();
            String[] orderedKeys = {
                    "集群名称", "HTTP接口", "Transport接口", "节点列表", "安全认证"
            };

            for (String key : orderedKeys) {
                if (basicInfo.containsKey(key)) {
                    Map<String, String> item = new HashMap<>();
                    item.put("label", key);
                    item.put("value", basicInfo.get(key));
                    basicInfoList.add(item);
                }
            }

            // 9. 构建完整的连接信息
            return ConnectionInfo.builder()
                    .basicInfo(basicInfo)
                    .basicInfoList(basicInfoList)
                    .javaCode(generateJavaCode(httpAddresses, clusterName, enableSecurity, securityUser,
                            securityPassword))
                    .pythonCode(generatePythonCode(httpAddresses, enableSecurity, securityUser, securityPassword))
                    .commandLines(
                            generateCommandLines(esHome, httpAddresses, enableSecurity, securityUser, securityPassword))
                    .hostName(esNodes.get(0))
                    .build();

        } catch (Exception e) {
            log.error("获取ElasticSearch连接信息出错: {}", e.getMessage(), e);
            return ConnectionInfo.builder().build();
        }
    }

    /**
     * 生成Java代码示例
     */
    private String generateJavaCode(String httpAddresses, String clusterName, boolean enableSecurity,
                                    String username, String password) {
        StringBuilder code = new StringBuilder();
        // 导入包
        code.append("import org.apache.http.HttpHost;\n");
        code.append("import org.apache.http.auth.AuthScope;\n");
        code.append("import org.apache.http.auth.UsernamePasswordCredentials;\n");
        code.append("import org.apache.http.client.CredentialsProvider;\n");
        code.append("import org.apache.http.impl.client.BasicCredentialsProvider;\n");
        code.append("import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;\n");
        code.append("import org.elasticsearch.action.index.IndexRequest;\n");
        code.append("import org.elasticsearch.action.index.IndexResponse;\n");
        code.append("import org.elasticsearch.action.search.SearchRequest;\n");
        code.append("import org.elasticsearch.action.search.SearchResponse;\n");
        code.append("import org.elasticsearch.client.RequestOptions;\n");
        code.append("import org.elasticsearch.client.RestClient;\n");
        code.append("import org.elasticsearch.client.RestClientBuilder;\n");
        code.append("import org.elasticsearch.client.RestHighLevelClient;\n");
        code.append("import org.elasticsearch.common.xcontent.XContentType;\n");
        code.append("import org.elasticsearch.index.query.QueryBuilders;\n");
        code.append("import org.elasticsearch.search.builder.SearchSourceBuilder;\n\n");

        code.append("import java.io.IOException;\n");
        code.append("import java.util.HashMap;\n");
        code.append("import java.util.Map;\n\n");

        // 类定义
        code.append("/**\n");
        code.append(" * ElasticSearch Java客户端示例\n");
        code.append(" */\n");
        code.append("public class ElasticSearchExample {\n\n");

        // main方法
        code.append("    public static void main(String[] args) {\n");
        code.append("        try (RestHighLevelClient client = createClient()) {\n");
        code.append("            // 索引文档示例\n");
        code.append("            indexDocument(client);\n\n");

        code.append("            // 搜索文档示例\n");
        code.append("            searchDocuments(client);\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            e.printStackTrace();\n");
        code.append("        }\n");
        code.append("    }\n\n");

        // 创建客户端方法
        code.append("    /**\n");
        code.append("     * 创建ElasticSearch客户端\n");
        code.append("     */\n");
        code.append("    private static RestHighLevelClient createClient() {\n");

        // 解析地址
        code.append("        // 解析ElasticSearch节点地址\n");
        code.append("        String[] addresses = \"").append(httpAddresses).append("\".split(\",\");\n");
        code.append("        HttpHost[] hosts = new HttpHost[addresses.length];\n");
        code.append("        for (int i = 0; i < addresses.length; i++) {\n");
        code.append("            String[] hostPort = addresses[i].split(\":\");\n");
        code.append("            hosts[i] = new HttpHost(hostPort[0], Integer.parseInt(hostPort[1]), \"http\");\n");
        code.append("        }\n\n");

        // 客户端构建
        code.append("        // 创建RestClientBuilder\n");
        code.append("        RestClientBuilder builder = RestClient.builder(hosts);\n\n");

        // 安全认证配置
        if (enableSecurity) {
            code.append("        // 配置安全认证\n");
            code.append("        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();\n");
            code.append("        credentialsProvider.setCredentials(AuthScope.ANY,\n");
            code.append("                new UsernamePasswordCredentials(\"").append(username).append("\", \"")
                    .append(password).append("\"));\n\n");

            code.append(
                    "        builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {\n");
            code.append("            @Override\n");
            code.append(
                    "            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {\n");
            code.append(
                    "                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);\n");
            code.append("            }\n");
            code.append("        });\n");
        }

        code.append("        return new RestHighLevelClient(builder);\n");
        code.append("    }\n\n");

        // 索引文档方法
        code.append("    /**\n");
        code.append("     * 索引文档示例\n");
        code.append("     */\n");
        code.append("    private static void indexDocument(RestHighLevelClient client) throws IOException {\n");
        code.append("        // 创建索引请求\n");
        code.append("        IndexRequest indexRequest = new IndexRequest(\"sample-index\");\n");
        code.append("        indexRequest.id(\"1\");\n\n");

        code.append("        // 准备文档数据\n");
        code.append("        Map<String, Object> jsonMap = new HashMap<>();\n");
        code.append("        jsonMap.put(\"name\", \"测试文档\");\n");
        code.append("        jsonMap.put(\"description\", \"这是一个使用Java客户端创建的测试文档\");\n");
        code.append("        jsonMap.put(\"timestamp\", System.currentTimeMillis());\n\n");

        code.append("        // 设置文档内容\n");
        code.append("        indexRequest.source(jsonMap);\n\n");

        code.append("        // 执行索引请求\n");
        code.append("        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);\n");
        code.append("        System.out.println(\"索引结果: \" + indexResponse.getResult());\n");
        code.append("    }\n\n");

        // 搜索文档方法
        code.append("    /**\n");
        code.append("     * 搜索文档示例\n");
        code.append("     */\n");
        code.append("    private static void searchDocuments(RestHighLevelClient client) throws IOException {\n");
        code.append("        // 创建搜索请求\n");
        code.append("        SearchRequest searchRequest = new SearchRequest(\"sample-index\");\n\n");

        code.append("        // 构建搜索条件\n");
        code.append("        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();\n");
        code.append("        sourceBuilder.query(QueryBuilders.matchQuery(\"name\", \"测试\"));\n");
        code.append("        searchRequest.source(sourceBuilder);\n\n");

        code.append("        // 执行搜索\n");
        code.append("        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);\n");
        code.append("        System.out.println(\"搜索结果总数: \" + searchResponse.getHits().getTotalHits().value);\n");
        code.append("        System.out.println(\"搜索结果: \" + searchResponse.getHits());\n");
        code.append("    }\n");

        code.append("}\n");

        return code.toString();
    }

    /**
     * 生成Python代码示例
     */
    private String generatePythonCode(String httpAddresses, boolean enableSecurity,
                                      String username, String password) {
        StringBuilder code = new StringBuilder();

        // 导入包
        code.append("from elasticsearch import Elasticsearch\n");
        code.append("from elasticsearch.helpers import scan\n");
        code.append("import json\n");
        code.append("from datetime import datetime\n\n");

        code.append("# ElasticSearch Python客户端示例\n\n");

        // 创建客户端
        code.append("def create_client():\n");
        code.append("    \"\"\"创建ElasticSearch客户端\"\"\"\n");
        code.append("    # 解析ElasticSearch节点地址\n");
        code.append("    hosts = \"").append(httpAddresses).append("\".split(\",\")\n\n");

        // 不同认证方式的客户端创建
        if (enableSecurity) {
            code.append("    # 创建带安全认证的客户端\n");
            code.append("    return Elasticsearch(\n");
            code.append("        hosts=hosts,\n");
            code.append("        http_auth=(\"").append(username).append("\", \"").append(password).append("\")\n");
            code.append("    )\n\n");
        } else {
            code.append("    # 创建普通客户端\n");
            code.append("    return Elasticsearch(hosts=hosts)\n\n");
        }

        // 索引文档方法
        code.append("def index_document(client):\n");
        code.append("    \"\"\"索引文档示例\"\"\"\n");
        code.append("    # 准备文档数据\n");
        code.append("    document = {\n");
        code.append("        'name': '测试文档',\n");
        code.append("        'description': '这是一个使用Python客户端创建的测试文档',\n");
        code.append("        'timestamp': datetime.now().isoformat()\n");
        code.append("    }\n\n");

        code.append("    # 索引文档\n");
        code.append("    response = client.index(\n");
        code.append("        index='sample-index',\n");
        code.append("        id=1,\n");
        code.append("        body=document\n");
        code.append("    )\n");
        code.append("    print(f\"索引结果: {response['result']}\")\n\n");

        // 搜索文档方法
        code.append("def search_documents(client):\n");
        code.append("    \"\"\"搜索文档示例\"\"\"\n");
        code.append("    # 执行搜索\n");
        code.append("    response = client.search(\n");
        code.append("        index='sample-index',\n");
        code.append("        body={\n");
        code.append("            'query': {\n");
        code.append("                'match': {\n");
        code.append("                    'name': '测试'\n");
        code.append("                }\n");
        code.append("            }\n");
        code.append("        }\n");
        code.append("    )\n\n");

        code.append("    # 处理搜索结果\n");
        code.append("    print(f\"搜索结果总数: {response['hits']['total']['value']}\")\n");
        code.append("    for hit in response['hits']['hits']:\n");
        code.append("        print(f\"文档ID: {hit['_id']}, 得分: {hit['_score']}\")\n");
        code.append("        print(f\"文档内容: {json.dumps(hit['_source'], ensure_ascii=False)}\")\n\n");

        // 查询集群状态方法
        code.append("def check_cluster_status(client):\n");
        code.append("    \"\"\"查询集群状态示例\"\"\"\n");
        code.append("    response = client.cluster.health()\n");
        code.append("    print(f\"集群名称: {response['cluster_name']}\")\n");
        code.append("    print(f\"集群状态: {response['status']}\")\n");
        code.append("    print(f\"节点数量: {response['number_of_nodes']}\")\n");
        code.append("    print(f\"数据节点数量: {response['number_of_data_nodes']}\")\n\n");

        // 主方法
        code.append("if __name__ == \"__main__\":\n");
        code.append("    # 创建客户端\n");
        code.append("    es = create_client()\n\n");

        code.append("    try:\n");
        code.append("        # 检查集群状态\n");
        code.append("        check_cluster_status(es)\n\n");

        code.append("        # 索引文档\n");
        code.append("        index_document(es)\n\n");

        code.append("        # 搜索文档\n");
        code.append("        search_documents(es)\n\n");

        code.append("    except Exception as e:\n");
        code.append("        print(f\"执行出错: {e}\")\n");

        return code.toString();
    }

    /**
     * 生成命令行示例
     */
    private List<CommandLineItem> generateCommandLines(String esHome, String httpAddresses,
                                                       boolean enableSecurity, String username, String password) {
        List<CommandLineItem> commands = new ArrayList<>();
        String[] addresses = httpAddresses.split(",");
        String hostname = addresses[0].split(":")[0];
        String port = addresses[0].split(":")[1];

        // 构建基础命令
        String authParam = enableSecurity ? " -u " + username + ":" + password : "";

        // 添加进入es目录的提示符
        String esHomePrompt = "[root@" + hostname + " " + esHome.substring(esHome.lastIndexOf('/') + 1) + "]# ";

        // 1. 检查集群健康状态
        CommandLineItem healthItem = new CommandLineItem();
        healthItem.setLabel("检查集群健康状态");
        healthItem.setValue(
                "curl" + authParam + " -X GET \"http://" + hostname + ":" + port + "/_cluster/health?pretty\"");
        healthItem.setCommandResult(
                "{\n  \"cluster_name\" : \"datasophon-es\",\n  \"status\" : \"green\",\n  \"timed_out\" : false,\n  \"number_of_nodes\" : 3,\n  \"number_of_data_nodes\" : 2,\n  \"active_primary_shards\" : 5,\n  \"active_shards\" : 10,\n  \"relocating_shards\" : 0,\n  \"initializing_shards\" : 0,\n  \"unassigned_shards\" : 0,\n  \"delayed_unassigned_shards\" : 0,\n  \"number_of_pending_tasks\" : 0,\n  \"number_of_in_flight_fetch\" : 0,\n  \"task_max_waiting_in_queue_millis\" : 0,\n  \"active_shards_percent_as_number\" : 100.0\n}");
        healthItem.setCommandPrompt(esHomePrompt);
        commands.add(healthItem);

        // 2. 列出所有索引
        CommandLineItem indicesItem = new CommandLineItem();
        indicesItem.setLabel("列出所有索引");
        indicesItem.setValue("curl" + authParam + " -X GET \"http://" + hostname + ":" + port + "/_cat/indices?v\"");
        indicesItem.setCommandResult(
                "health status index                           uuid                   pri rep docs.count docs.deleted store.size pri.store.size\ngreen  open   .kibana_task_manager_7.17.7_001 BIaAU-iiS1-VL1EdZ2QEYw   1   1          0            0       566b           283b\ngreen  open   .kibana_7.17.7_001              FhkGBuQlRCuAgPlGd0LTZQ   1   1          0            0       566b           283b\nyellow open   my-index-000001                 XqWg9IviRISPSJnDAAlacg   1   1          0            0        226b           226b");
        indicesItem.setCommandPrompt(esHomePrompt);
        commands.add(indicesItem);

        // 3. 创建索引
        CommandLineItem createIndexItem = new CommandLineItem();
        createIndexItem.setLabel("创建索引");
        createIndexItem.setValue("curl" + authParam + " -X PUT \"http://" + hostname + ":" + port
                + "/test-index\" -H \"Content-Type: application/json\" -d'\n{\n  \"settings\": {\n    \"number_of_shards\": 1,\n    \"number_of_replicas\": 1\n  },\n  \"mappings\": {\n    \"properties\": {\n      \"name\": { \"type\": \"text\" },\n      \"age\": { \"type\": \"integer\" },\n      \"created\": { \"type\": \"date\" }\n    }\n  }\n}\n'");
        createIndexItem.setCommandResult(
                "{\n  \"acknowledged\" : true,\n  \"shards_acknowledged\" : true,\n  \"index\" : \"test-index\"\n}");
        createIndexItem.setCommandPrompt(esHomePrompt);
        commands.add(createIndexItem);

        // 4. 索引文档
        CommandLineItem indexDocItem = new CommandLineItem();
        indexDocItem.setLabel("索引文档");
        indexDocItem.setValue("curl" + authParam + " -X POST \"http://" + hostname + ":" + port
                + "/test-index/_doc/1\" -H \"Content-Type: application/json\" -d'\n{\n  \"name\": \"测试文档\",\n  \"age\": 28,\n  \"created\": \"2023-01-15T12:10:30Z\"\n}\n'");
        indexDocItem.setCommandResult(
                "{\n  \"_index\" : \"test-index\",\n  \"_id\" : \"1\",\n  \"_version\" : 1,\n  \"result\" : \"created\",\n  \"_shards\" : {\n    \"total\" : 2,\n    \"successful\" : 2,\n    \"failed\" : 0\n  },\n  \"_seq_no\" : 0,\n  \"_primary_term\" : 1\n}");
        indexDocItem.setCommandPrompt(esHomePrompt);
        commands.add(indexDocItem);

        // 5. 查询文档
        CommandLineItem getDocItem = new CommandLineItem();
        getDocItem.setLabel("查询文档");
        getDocItem.setValue(
                "curl" + authParam + " -X GET \"http://" + hostname + ":" + port + "/test-index/_doc/1?pretty\"");
        getDocItem.setCommandResult(
                "{\n  \"_index\" : \"test-index\",\n  \"_id\" : \"1\",\n  \"_version\" : 1,\n  \"_seq_no\" : 0,\n  \"_primary_term\" : 1,\n  \"found\" : true,\n  \"_source\" : {\n    \"name\" : \"测试文档\",\n    \"age\" : 28,\n    \"created\" : \"2023-01-15T12:10:30Z\"\n  }\n}");
        getDocItem.setCommandPrompt(esHomePrompt);
        commands.add(getDocItem);

        // 6. 搜索文档
        CommandLineItem searchItem = new CommandLineItem();
        searchItem.setLabel("搜索文档");
        searchItem.setValue("curl" + authParam + " -X GET \"http://" + hostname + ":" + port
                + "/test-index/_search?pretty\" -H \"Content-Type: application/json\" -d'\n{\n  \"query\": {\n    \"match\": {\n      \"name\": \"测试\"\n    }\n  }\n}\n'");
        searchItem.setCommandResult(
                "{\n  \"took\" : 5,\n  \"timed_out\" : false,\n  \"_shards\" : {\n    \"total\" : 1,\n    \"successful\" : 1,\n    \"skipped\" : 0,\n    \"failed\" : 0\n  },\n  \"hits\" : {\n    \"total\" : {\n      \"value\" : 1,\n      \"relation\" : \"eq\"\n    },\n    \"max_score\" : 1.0,\n    \"hits\" : [\n      {\n        \"_index\" : \"test-index\",\n        \"_id\" : \"1\",\n        \"_score\" : 1.0,\n        \"_source\" : {\n          \"name\" : \"测试文档\",\n          \"age\" : 28,\n          \"created\" : \"2023-01-15T12:10:30Z\"\n        }\n      }\n    ]\n  }\n}");
        searchItem.setCommandPrompt(esHomePrompt);
        commands.add(searchItem);

        // 7. 删除文档
        CommandLineItem deleteDocItem = new CommandLineItem();
        deleteDocItem.setLabel("删除文档");
        deleteDocItem.setValue(
                "curl" + authParam + " -X DELETE \"http://" + hostname + ":" + port + "/test-index/_doc/1?pretty\"");
        deleteDocItem.setCommandResult(
                "{\n  \"_index\" : \"test-index\",\n  \"_id\" : \"1\",\n  \"_version\" : 2,\n  \"result\" : \"deleted\",\n  \"_shards\" : {\n    \"total\" : 2,\n    \"successful\" : 2,\n    \"failed\" : 0\n  },\n  \"_seq_no\" : 1,\n  \"_primary_term\" : 1\n}");
        deleteDocItem.setCommandPrompt(esHomePrompt);
        commands.add(deleteDocItem);

        // 8. 查看集群节点
        CommandLineItem nodesItem = new CommandLineItem();
        nodesItem.setLabel("查看集群节点");
        nodesItem.setValue("curl" + authParam + " -X GET \"http://" + hostname + ":" + port + "/_cat/nodes?v\"");
        nodesItem.setCommandResult(
                "ip        heap.percent ram.percent cpu load_1m load_5m load_15m node.role   master name\n127.0.0.1           65          95   4    2.34    2.32     2.56 cdfhilmrstw *      es1\n127.0.0.2           42          93   3    2.05    1.98     2.45 cdfhilmrstw -      es2\n127.0.0.3           50          91   2    1.46    1.54     1.97 cdfhilmrstw -      es3");
        nodesItem.setCommandPrompt(esHomePrompt);
        commands.add(nodesItem);

        // 9. 查看集群分片分配
        CommandLineItem shardsItem = new CommandLineItem();
        shardsItem.setLabel("查看集群分片分配");
        shardsItem.setValue("curl" + authParam + " -X GET \"http://" + hostname + ":" + port + "/_cat/shards?v\"");
        shardsItem.setCommandResult(
                "index                           shard prirep state   docs  store ip        node\n.kibana_task_manager_7.17.7_001 0     p      STARTED    0   283b 127.0.0.1 es1\n.kibana_task_manager_7.17.7_001 0     r      STARTED    0   283b 127.0.0.2 es2\n.kibana_7.17.7_001              0     p      STARTED    0   283b 127.0.0.1 es1\n.kibana_7.17.7_001              0     r      STARTED    0   283b 127.0.0.3 es3\ntest-index                      0     p      STARTED    0   226b 127.0.0.2 es2\ntest-index                      0     r      STARTED    0   226b 127.0.0.3 es3");
        shardsItem.setCommandPrompt(esHomePrompt);
        commands.add(shardsItem);

        // 10. 使用elasticsearch-plugin管理插件
        CommandLineItem pluginItem = new CommandLineItem();
        pluginItem.setLabel("查看已安装的插件");
        pluginItem.setValue(esHome + "/bin/elasticsearch-plugin list");
        pluginItem.setCommandResult("analysis-icu\nrepository-s3\ningest-geoip");
        pluginItem.setCommandPrompt(esHomePrompt);
        commands.add(pluginItem);

        return addFinalPrompt(commands, esHome, hostname);
    }


}
