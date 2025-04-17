DEPENDENCIES_START
<!-- Maven依赖： -->
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>7.16.2</version>
</dependency>
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-client</artifactId>
    <version>7.16.2</version>
</dependency>
<dependency>
    <groupId>org.elasticsearch</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>7.16.2</version>
</dependency>
DEPENDENCIES_END

package com.example.elasticsearch;

/*
 * ElasticSearch Java连接示例
 */

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ElasticSearch连接示例
 */
public class ElasticSearchExample {

    public static void main(String[] args) {
        // 连接参数
        String host = "${data.getBasicInfoValue('host', 'localhost')}";
        int httpPort = Integer.parseInt("${data.getBasicInfoValue('httpPort', '9200')}");
        String clusterName = "${data.getBasicInfoValue('clusterName', 'elasticsearch')}";
        
        // 如果有多个节点，使用nodeList
        String nodeList = "${data.getBasicInfoValue('nodeList', '')}";
        
        // 安全认证配置
        String authMode = "${data.getSecurityInfoValue('authMode', '无认证')}";
        String username = "${data.getSecurityInfoValue('username', '')}";
        String password = "${data.getSecurityInfoValue('password', '')}";
        
        System.out.println("===== ElasticSearch连接示例 =====");
        System.out.println("主机: " + host);
        System.out.println("HTTP端口: " + httpPort);
        System.out.println("集群名称: " + clusterName);
        
        RestHighLevelClient client = null;
        
        try {
            // 创建客户端连接
            client = createClient(host, httpPort, nodeList, authMode, username, password);
            
            System.out.println("ElasticSearch连接创建成功");
            
            // 检查集群健康状态
            checkClusterHealth(client);
            
            // 索引操作示例
            indexOperationsExample(client);
            
            // 文档操作示例
            documentOperationsExample(client);
            
            // 搜索操作示例
            searchOperationsExample(client);
            
            // 批量操作示例
            bulkOperationsExample(client);
            
        } catch (Exception e) {
            System.err.println("ElasticSearch操作出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭客户端
            if (client != null) {
                try {
                    client.close();
                    System.out.println("ElasticSearch客户端已关闭");
                } catch (IOException e) {
                    System.err.println("关闭客户端时出错: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 创建ElasticSearch客户端
     */
    private static RestHighLevelClient createClient(String host, int port, String nodeList, 
                                                   String authMode, String username, String password) {
        RestClientBuilder builder;
        
        // 判断是单节点还是多节点
        if (nodeList != null && !nodeList.isEmpty()) {
            // 多节点模式
            String[] nodes = nodeList.split(",");
            HttpHost[] httpHosts = new HttpHost[nodes.length];
            
            for (int i = 0; i < nodes.length; i++) {
                String[] hostPort = nodes[i].split(":");
                String nodeHost = hostPort[0];
                int nodePort = (hostPort.length > 1) ? Integer.parseInt(hostPort[1]) : port;
                httpHosts[i] = new HttpHost(nodeHost, nodePort, "http");
            }
            
            builder = RestClient.builder(httpHosts);
        } else {
            // 单节点模式
            builder = RestClient.builder(new HttpHost(host, port, "http"));
        }
        
        // 如果启用了用户名密码认证
        if ("用户名密码".equals(authMode) && username != null && !username.isEmpty()) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            
            builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                @Override
                public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
            });
        }
        
        // 设置连接超时和Socket超时
        builder.setRequestConfigCallback(requestConfigBuilder -> 
            requestConfigBuilder
                .setConnectTimeout(5000)  // 连接超时时间
                .setSocketTimeout(60000)); // Socket读取超时时间
        
        return new RestHighLevelClient(builder);
    }
    
    /**
     * 检查集群健康状态
     */
    private static void checkClusterHealth(RestHighLevelClient client) throws IOException {
        System.out.println("\n===== 集群健康状态 =====");
        
        // 获取集群信息
        org.elasticsearch.client.core.MainResponse info = client.info(RequestOptions.DEFAULT);
        System.out.println("集群名称: " + info.getClusterName());
        System.out.println("集群版本: " + info.getVersion().getNumber());
        System.out.println("Lucene版本: " + info.getVersion().getLuceneVersion());
    }
    
    /**
     * 索引操作示例
     */
    private static void indexOperationsExample(RestHighLevelClient client) throws IOException {
        System.out.println("\n===== 索引操作示例 =====");
        
        String indexName = "user_index";
        
        // 检查索引是否存在
        boolean exists = client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
        if (exists) {
            System.out.println("索引 " + indexName + " 已存在，删除它");
            AcknowledgedResponse deleteResponse = client.indices().delete(
                new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
            System.out.println("删除索引响应: " + deleteResponse.isAcknowledged());
        }
        
        // 创建索引
        CreateIndexRequest createRequest = new CreateIndexRequest(indexName);
        createRequest.settings(Settings.builder()
                .put("index.number_of_shards", 3)
                .put("index.number_of_replicas", 2));
        
        // 设置映射
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> nameField = new HashMap<>();
        nameField.put("type", "text");
        nameField.put("analyzer", "standard");
        
        Map<String, Object> ageField = new HashMap<>();
        ageField.put("type", "integer");
        
        Map<String, Object> emailField = new HashMap<>();
        emailField.put("type", "keyword");
        
        Map<String, Object> birthdayField = new HashMap<>();
        birthdayField.put("type", "date");
        birthdayField.put("format", "yyyy-MM-dd");
        
        properties.put("name", nameField);
        properties.put("age", ageField);
        properties.put("email", emailField);
        properties.put("birthday", birthdayField);
        
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);
        
        createRequest.mapping(mapping);
        
        // 执行创建索引请求
        CreateIndexResponse createResponse = client.indices().create(createRequest, RequestOptions.DEFAULT);
        System.out.println("创建索引响应: " + createResponse.isAcknowledged());
    }
    
    /**
     * 文档操作示例
     */
    private static void documentOperationsExample(RestHighLevelClient client) throws IOException {
        System.out.println("\n===== 文档操作示例 =====");
        
        String indexName = "user_index";
        String documentId = "1";
        
        // 创建文档
        Map<String, Object> document = new HashMap<>();
        document.put("name", "张三");
        document.put("age", 30);
        document.put("email", "zhangsan@example.com");
        document.put("birthday", "1990-01-01");
        
        // 索引文档
        IndexRequest indexRequest = new IndexRequest(indexName)
                .id(documentId)
                .source(document);
        
        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
        System.out.println("索引文档响应: " + indexResponse.getResult());
        
        // 获取文档
        GetRequest getRequest = new GetRequest(indexName, documentId);
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        System.out.println("获取文档: " + getResponse.getSourceAsString());
        
        // 更新文档
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("age", 31);
        
        UpdateRequest updateRequest = new UpdateRequest(indexName, documentId)
                .doc(updateMap);
        
        UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
        System.out.println("更新文档响应: " + updateResponse.getResult());
        
        // 再次获取文档
        getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        System.out.println("更新后的文档: " + getResponse.getSourceAsString());
        
        // 检查文档是否存在
        GetRequest existsRequest = new GetRequest(indexName, documentId);
        existsRequest.fetchSourceContext(new FetchSourceContext(false));
        existsRequest.storedFields("_none_");
        
        boolean exists = client.exists(existsRequest, RequestOptions.DEFAULT);
        System.out.println("文档是否存在: " + exists);
        
        // 删除文档
        DeleteRequest deleteRequest = new DeleteRequest(indexName, documentId);
        DeleteResponse deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
        System.out.println("删除文档响应: " + deleteResponse.getResult());
    }
    
    /**
     * 搜索操作示例
     */
    private static void searchOperationsExample(RestHighLevelClient client) throws IOException {
        System.out.println("\n===== 搜索操作示例 =====");
        
        String indexName = "user_index";
        
        // 创建一些测试数据
        BulkRequest bulkRequest = new BulkRequest();
        
        Map<String, Object> user1 = new HashMap<>();
        user1.put("name", "张三");
        user1.put("age", 30);
        user1.put("email", "zhangsan@example.com");
        user1.put("birthday", "1990-01-01");
        
        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "李四");
        user2.put("age", 25);
        user2.put("email", "lisi@example.com");
        user2.put("birthday", "1995-05-05");
        
        Map<String, Object> user3 = new HashMap<>();
        user3.put("name", "王五");
        user3.put("age", 35);
        user3.put("email", "wangwu@example.com");
        user3.put("birthday", "1985-10-10");
        
        bulkRequest.add(new IndexRequest(indexName).id("1").source(user1));
        bulkRequest.add(new IndexRequest(indexName).id("2").source(user2));
        bulkRequest.add(new IndexRequest(indexName).id("3").source(user3));
        
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        
        // 执行搜索 - 精确查询
        System.out.println("\n----- 精确查询 -----");
        SearchRequest exactSearchRequest = new SearchRequest(indexName);
        SearchSourceBuilder exactSourceBuilder = new SearchSourceBuilder();
        
        MatchQueryBuilder exactMatchQuery = QueryBuilders.matchQuery("name", "李四");
        exactSourceBuilder.query(exactMatchQuery);
        exactSearchRequest.source(exactSourceBuilder);
        
        SearchResponse exactSearchResponse = client.search(exactSearchRequest, RequestOptions.DEFAULT);
        System.out.println("精确查询结果:");
        for (SearchHit hit : exactSearchResponse.getHits().getHits()) {
            System.out.println(hit.getSourceAsString());
        }
        
        // 执行搜索 - 范围查询
        System.out.println("\n----- 范围查询 -----");
        SearchRequest rangeSearchRequest = new SearchRequest(indexName);
        SearchSourceBuilder rangeSourceBuilder = new SearchSourceBuilder();
        
        rangeSourceBuilder.query(QueryBuilders.rangeQuery("age").gte(25).lte(33));
        rangeSearchRequest.source(rangeSourceBuilder);
        
        SearchResponse rangeSearchResponse = client.search(rangeSearchRequest, RequestOptions.DEFAULT);
        System.out.println("范围查询结果:");
        for (SearchHit hit : rangeSearchResponse.getHits().getHits()) {
            System.out.println(hit.getSourceAsString());
        }
        
        // 执行搜索 - 排序
        System.out.println("\n----- 排序查询 -----");
        SearchRequest sortSearchRequest = new SearchRequest(indexName);
        SearchSourceBuilder sortSourceBuilder = new SearchSourceBuilder();
        
        sortSourceBuilder.query(QueryBuilders.matchAllQuery())
                        .sort("age", SortOrder.DESC);
        sortSearchRequest.source(sortSourceBuilder);
        
        SearchResponse sortSearchResponse = client.search(sortSearchRequest, RequestOptions.DEFAULT);
        System.out.println("排序查询结果 (按年龄降序):");
        for (SearchHit hit : sortSearchResponse.getHits().getHits()) {
            System.out.println(hit.getSourceAsString());
        }
        
        // 执行搜索 - 模糊查询
        System.out.println("\n----- 模糊查询 -----");
        SearchRequest fuzzySearchRequest = new SearchRequest(indexName);
        SearchSourceBuilder fuzzySourceBuilder = new SearchSourceBuilder();
        
        fuzzySourceBuilder.query(QueryBuilders.fuzzyQuery("name", "张山").fuzziness(Fuzziness.AUTO));
        fuzzySearchRequest.source(fuzzySourceBuilder);
        
        SearchResponse fuzzySearchResponse = client.search(fuzzySearchRequest, RequestOptions.DEFAULT);
        System.out.println("模糊查询结果 (搜索'张山'，实际找到'张三'):");
        for (SearchHit hit : fuzzySearchResponse.getHits().getHits()) {
            System.out.println(hit.getSourceAsString());
        }
    }
    
    /**
     * 批量操作示例
     */
    private static void bulkOperationsExample(RestHighLevelClient client) throws IOException {
        System.out.println("\n===== 批量操作示例 =====");
        
        String indexName = "product_index";
        
        // 删除之前的索引(如果存在)
        boolean exists = client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
        if (exists) {
            client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        }
        
        // 创建新索引
        CreateIndexRequest createRequest = new CreateIndexRequest(indexName);
        client.indices().create(createRequest, RequestOptions.DEFAULT);
        
        // 准备批量请求
        BulkRequest bulkRequest = new BulkRequest();
        
        // 添加多条数据
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> product = new HashMap<>();
            product.put("name", "产品" + i);
            product.put("price", 100 + i * 10);
            product.put("category", (i % 3 == 0) ? "电子" : (i % 3 == 1) ? "家具" : "服装");
            product.put("createTime", new Date().toString());
            
            bulkRequest.add(new IndexRequest(indexName)
                    .id(String.valueOf(i))
                    .source(product));
        }
        
        // 执行批量操作
        BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        
        System.out.println("批量操作响应: " + !bulkResponse.hasFailures());
        System.out.println("批量操作耗时: " + bulkResponse.getTook().getMillis() + "ms");
        
        if (bulkResponse.hasFailures()) {
            System.out.println("批量操作中有失败: " + bulkResponse.buildFailureMessage());
        }
        
        // 查询所有产品
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(searchSourceBuilder);
        
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("\n所有产品列表:");
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            System.out.println(hit.getSourceAsString());
        }
    }
} 