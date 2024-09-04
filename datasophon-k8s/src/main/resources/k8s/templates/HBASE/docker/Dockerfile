FROM 192.168.5.200:5000/bdp/jdk:1.8.0_333

RUN groupadd -r -g 2001 hadoop \
    && useradd -r -u 2002 -g 2001 -m yarn \
    && useradd -r -u 2001 -g 2001 -m hdfs \
    && useradd -r -u 2005 -g 2001 -m hbase \
    && mkdir -p /opt/datasophon

WORKDIR /opt/datasophon

ADD hadoop-3.3.3.tar.gz /opt/datasophon/
ADD hbase-2.2.7.tar.gz /opt/datasophon/

RUN chown -R root:hadoop /opt/datasophon/hadoop-3.3.3 \
    && chmod -R 775 /opt/datasophon/hadoop-3.3.3 \
    && chmod 6050 /opt/datasophon/hadoop-3.3.3/bin/container-executor \
    && chmod 400 /opt/datasophon/hadoop-3.3.3/etc/hadoop/container-executor.cfg \
    && chown -R yarn:hadoop /opt/datasophon/hadoop-3.3.3/logs/userlogs \
    && chmod 775 /opt/datasophon/hadoop-3.3.3/logs/userlogs \
    && chmod -R 775 /opt/datasophon/hbase-2.2.7 \
    && chmod 777 /opt/datasophon/hbase-2.2.7/logs \
    && chown -R hbase:hadoop /opt/datasophon/hbase-2.2.7

ENV HADOOP_HOME=/opt/datasophon/hadoop-3.3.3 \
    HADOOP_CONF_DIR=/opt/datasophon/hadoop-3.3.3/etc/hadoop \
    HBASE_HOME=/opt/datasophon/hbase-2.2.7 \
    PATH=$PATH:$HADOOP_HOME/bin:$HBASE_HOME/bin \
    HADOOP_USER_NAME=hdfs