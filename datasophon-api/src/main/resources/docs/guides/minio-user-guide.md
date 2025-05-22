# MinIO 用户指南

## 环境准备

### 系统要求

在安装 MinIO 之前，请确保您的系统满足以下要求：

- 64 位架构的操作系统（Linux、Windows、macOS）
- 最低 2GB RAM（生产环境建议 8GB 以上）
- 足够的磁盘空间（取决于存储需求）
- 网络连接（集群模式需要节点间通信）

### 安装配置

1. **下载 MinIO**
   ```bash
   wget https://dl.min.io/server/minio/release/linux-amd64/minio
   chmod +x minio
   ```

2. **单节点部署**
   ```bash
   ./minio server /data
   ```

3. **集群部署**
   ```bash
   ./minio server http://minio{1...4}/data{1...2}
   ```

4. **环境变量配置**
   ```bash
   export MINIO_ROOT_USER=admin
   export MINIO_ROOT_PASSWORD=password
   ```

## 基本操作

### 访问 MinIO

1. **Web 控制台**
   - 默认地址：http://localhost:9000
   - 使用 MINIO_ROOT_USER 和 MINIO_ROOT_PASSWORD 登录

2. **命令行工具**
   ```bash
   # 安装 MinIO 客户端
   wget https://dl.min.io/client/mc/release/linux-amd64/mc
   chmod +x mc
   
   # 配置 MinIO 客户端
   ./mc alias set myminio http://localhost:9000 admin password
   ```

### 存储桶管理

1. **创建存储桶**
   ```bash
   ./mc mb myminio/mybucket
   ```

2. **列出存储桶**
   ```bash
   ./mc ls myminio
   ```

3. **删除存储桶**
   ```bash
   ./mc rb myminio/mybucket
   ```

### 对象操作

1. **上传对象**
   ```bash
   ./mc cp myfile.txt myminio/mybucket
   ```

2. **下载对象**
   ```bash
   ./mc cp myminio/mybucket/myfile.txt myfile-copy.txt
   ```

3. **列出对象**
   ```bash
   ./mc ls myminio/mybucket
   ```

4. **删除对象**
   ```bash
   ./mc rm myminio/mybucket/myfile.txt
   ```

## 高级功能

### 访问控制

1. **创建用户**
   ```bash
   ./mc admin user add myminio newuser newpassword
   ```

2. **创建策略**
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": ["s3:GetObject"],
         "Resource": ["arn:aws:s3:::mybucket/*"]
       }
     ]
   }
   ```
   ```bash
   ./mc admin policy add myminio readonlypolicy /path/to/policy.json
   ```

3. **分配策略**
   ```bash
   ./mc admin policy set myminio readonlypolicy user=newuser
   ```

### 加密配置

1. **服务端加密**
   ```bash
   export MINIO_KMS_KES_ENDPOINT=https://kes-server:7373
   export MINIO_KMS_KES_KEY_FILE=/path/to/key.pem
   export MINIO_KMS_KES_CERT_FILE=/path/to/cert.pem
   export MINIO_KMS_KES_KEY_NAME=my-minio-key
   ```

2. **客户端加密**
   ```bash
   ./mc encrypt set myminio/mybucket
   ```

### 生命周期管理

1. **配置生命周期规则**
   ```json
   {
     "Rules": [
       {
         "ID": "Expire old logs",
         "Status": "Enabled",
         "Filter": {
           "Prefix": "logs/"
         },
         "Expiration": {
           "Days": 30
         }
       }
     ]
   }
   ```
   ```bash
   ./mc ilm import myminio/mybucket < lifecycle.json
   ```

2. **查看生命周期规则**
   ```bash
   ./mc ilm ls myminio/mybucket
   ```

## 监控与管理

### 性能监控

1. **Prometheus 集成**
   ```bash
   export MINIO_PROMETHEUS_AUTH_TYPE=public
   ```

2. **Grafana 仪表板**
   - 导入 MinIO 官方 Grafana 仪表板
   - 配置 Prometheus 数据源
   - 自定义监控面板

### 日志管理

1. **日志配置**
   ```bash
   export MINIO_LOGGER_WEBHOOK_ENABLE=on
   export MINIO_LOGGER_WEBHOOK_ENDPOINT=http://localhost:8080/minio/logs
   ```

2. **审计日志**
   ```bash
   export MINIO_AUDIT_WEBHOOK_ENABLE=on
   export MINIO_AUDIT_WEBHOOK_ENDPOINT=http://localhost:8080/minio/audit
   ```

### 备份与恢复

1. **数据备份**
   ```bash
   ./mc mirror myminio/mybucket backup/mybucket
   ```

2. **数据恢复**
   ```bash
   ./mc mirror backup/mybucket myminio/mybucket
   ```

## 开发集成

### S3 兼容 API

1. **Java SDK 示例**
   ```java
   import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
   import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
   import software.amazon.awssdk.regions.Region;
   import software.amazon.awssdk.services.s3.S3Client;
   import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
   import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
   
   public class MinioExample {
       public static void main(String[] args) {
           // 创建凭证
           AwsBasicCredentials credentials = AwsBasicCredentials.create(
               "admin", "password"
           );
           
           // 创建 S3 客户端
           S3Client s3 = S3Client.builder()
               .endpointOverride(URI.create("http://localhost:9000"))
               .credentialsProvider(StaticCredentialsProvider.create(credentials))
               .region(Region.US_EAST_1)
               .build();
           
           // 列出所有存储桶
           ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
           ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);
           listBucketsResponse.buckets().forEach(bucket -> {
               System.out.println(bucket.name());
           });
       }
   }
   ```

2. **Python SDK 示例**
   ```python
   import boto3
   from botocore.client import Config
   
   # 创建 S3 客户端
   s3 = boto3.client(
       's3',
       endpoint_url='http://localhost:9000',
       aws_access_key_id='admin',
       aws_secret_access_key='password',
       config=Config(signature_version='s3v4'),
       region_name='us-east-1'
   )
   
   # 列出所有存储桶
   response = s3.list_buckets()
   for bucket in response['Buckets']:
       print(bucket['Name'])
   ```

### MinIO SDK

1. **JavaScript SDK 示例**
   ```javascript
   const Minio = require('minio')
   
   // 创建客户端
   const minioClient = new Minio.Client({
       endPoint: 'localhost',
       port: 9000,
       useSSL: false,
       accessKey: 'admin',
       secretKey: 'password'
   });
   
   // 列出所有存储桶
   minioClient.listBuckets((err, buckets) => {
       if (err) return console.log(err)
       console.log('Buckets:')
       buckets.forEach((bucket) => {
           console.log(bucket.name)
       })
   })
   ```

2. **Go SDK 示例**
   ```go
   package main
   
   import (
       "context"
       "fmt"
       "log"
   
       "github.com/minio/minio-go/v7"
       "github.com/minio/minio-go/v7/pkg/credentials"
   )
   
   func main() {
       // 创建客户端
       endpoint := "localhost:9000"
       accessKeyID := "admin"
       secretAccessKey := "password"
       useSSL := false
   
       minioClient, err := minio.New(endpoint, &minio.Options{
           Creds:  credentials.NewStaticV4(accessKeyID, secretAccessKey, ""),
           Secure: useSSL,
       })
       if err != nil {
           log.Fatalln(err)
       }
   
       // 列出所有存储桶
       buckets, err := minioClient.ListBuckets(context.Background())
       if err != nil {
           log.Fatalln(err)
       }
       for _, bucket := range buckets {
           fmt.Println(bucket.Name)
       }
   }
   ```

## 高级配置

### 纠删码配置

1. **配置纠删码**
   ```bash
   ./minio server --erasure-code-parity 2 /data{1...8}
   ```

2. **纠删码参数**
   - 标准纠删码：EC:4
   - 高冗余纠删码：EC:2

### 缓存配置

1. **磁盘缓存**
   ```bash
   export MINIO_CACHE_DRIVES="/mnt/cache1,/mnt/cache2"
   export MINIO_CACHE_EXCLUDE="*.pdf,*.mp4"
   export MINIO_CACHE_QUOTA=90
   export MINIO_CACHE_AFTER=3
   export MINIO_CACHE_WATERMARK_LOW=75
   export MINIO_CACHE_WATERMARK_HIGH=85
   ```

2. **内存缓存**
   ```bash
   export MINIO_CACHE_QUOTA_SYSTEM=50
   ```

### 网关模式

1. **S3 网关**
   ```bash
   export MINIO_ROOT_USER=admin
   export MINIO_ROOT_PASSWORD=password
   ./minio gateway s3 https://s3.amazonaws.com
   ```

2. **Azure 网关**
   ```bash
   export MINIO_ROOT_USER=admin
   export MINIO_ROOT_PASSWORD=password
   export AZURE_STORAGE_ACCOUNT=azureaccountname
   export AZURE_STORAGE_KEY=azureaccountkey
   ./minio gateway azure
   ```

## 最佳实践

### 性能优化

1. **硬件选择**
   - 使用 SSD 或 NVMe 存储
   - 配置足够的内存（至少总存储空间的 1%）
   - 使用高速网络（10GbE 或更高）

2. **系统优化**
   ```bash
   # 增加文件描述符限制
   ulimit -n 65536
   
   # 调整内核参数
   sysctl -w net.core.somaxconn=65535
   sysctl -w net.ipv4.tcp_max_syn_backlog=65535
   ```

3. **应用优化**
   - 使用合适的分片大小（通常 128MB 或更大）
   - 启用压缩（对于文本数据）
   - 使用并行上传/下载

### 安全加固

1. **TLS 配置**
   ```bash
   export MINIO_CERT_FILE=/path/to/cert.pem
   export MINIO_KEY_FILE=/path/to/key.pem
   ```

2. **防火墙配置**
   ```bash
   # 仅允许必要的端口
   iptables -A INPUT -p tcp --dport 9000 -j ACCEPT
   iptables -A INPUT -p tcp --dport 9001 -j ACCEPT
   ```

3. **安全策略**
   - 定期轮换访问密钥
   - 实施最小权限原则
   - 启用审计日志

### 容灾备份

1. **跨区域复制**
   ```bash
   ./mc admin bucket remote add myminio/mybucket dest https://minio2:9000/backup
   ./mc admin bucket remote ls myminio/mybucket
   ```

2. **定期备份**
   ```bash
   # 创建定时任务
   crontab -e
   
   # 添加每日备份
   0 0 * * * /path/to/mc mirror myminio/mybucket backup/mybucket
   ```

3. **版本控制**
   ```bash
   ./mc version enable myminio/mybucket
   ```

## 故障排除

### 常见问题

1. **连接问题**
   - 检查网络连接
   - 验证端口是否开放
   - 确认防火墙设置

2. **权限问题**
   - 检查用户权限
   - 验证策略配置
   - 确认存储桶策略

3. **性能问题**
   - 监控磁盘 I/O
   - 检查网络带宽
   - 分析系统负载

### 诊断工具

1. **健康检查**
   ```bash
   ./mc admin info myminio
   ```

2. **日志分析**
   ```bash
   ./mc admin logs myminio
   ```

3. **性能分析**
   ```bash
   ./mc admin trace myminio
   ```

本指南涵盖了 MinIO 的主要使用方法和最佳实践。随着 MinIO 的持续发展，建议定期查看官方文档以获取最新信息和更新。在实际使用中，请根据具体需求和环境调整配置参数。