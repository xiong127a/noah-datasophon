# 使用统一的JDK软链接路径，支持多版本JDK（JDK8/11/17/21等）
# 实际JDK目录由环境检查修复步骤自动创建软链接：/usr/local/jdk -> /usr/local/jdk-x.x.x
export JAVA_HOME=/usr/local/jdk
CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
export JAVA_HOME CLASSPATH

export KYUUBI_HOME=/opt/datasophon/kyuubi-1.7.3
export SPARK_HOME=/opt/datasophon/spark-3.2.2
export PYSPARK_ALLOW_INSECURE_GATEWAY=1
export HIVE_HOME=/opt/datasophon/hive-3.1.0

export KAFKA_HOME=/opt/datasophon/kafka-2.4.1
export HBASE_HOME=/opt/datasophon/hbase-2.2.7
export FLINK_HOME=/opt/datasophon/flink-1.16.2
export HADOOP_HOME=/opt/datasophon/hadoop-3.3.3
export HADOOP_CONF_DIR=/opt/datasophon/hadoop-3.3.3/etc/hadoop
export PATH=$PATH:$JAVA_HOME/bin:$SPARK_HOME/bin:$HADOOP_HOME/bin:$HIVE_HOME/bin:$FLINK_HOME/bin:$KAFKA_HOME/bin:$HBASE_HOME/bin

# 只在hadoop命令可用时设置HADOOP_CLASSPATH
if command -v hadoop >/dev/null 2>&1; then
    export HADOOP_CLASSPATH=`hadoop classpath`
    
    export TEZ_CONF_DIR=$HADOOP_CONF_DIR
    export TEZ_JARS=/opt/datasophon/tez/*:/opt/datasophon/tez/lib/*
    export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:$TEZ_CONF_DIR:$TEZ_JARS
fi