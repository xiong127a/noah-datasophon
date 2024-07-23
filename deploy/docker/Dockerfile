FROM 192.168.5.200:5000/bdp/openjdk:8
RUN mkdir /datasophon-manager
COPY ./bin/ /datasophon-manager/bin/
COPY ./conf/ /datasophon-manager/conf/
COPY ./lib/ /datasophon-manager/lib/
COPY ./jmx/ /datasophon-manager/jmx/
ENV TZ=Asia/Shanghai
EXPOSE 8081
EXPOSE 8586
EXPOSE 2551
EXPOSE 8000
WORKDIR /datasophon-manager
ENTRYPOINT java -server -Xms1G -Xmx1G -Xmn512m \
-Dspring.config.location=/datasophon-manager/conf/application.yml \
-Dfile.encoding=UTF-8 \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 \
-javaagent:/datasophon-manager/jmx/jmx_prometheus_javaagent-0.16.1.jar=8586:/datasophon-manager/jmx/jmx_exporter_config.yaml \
-classpath /datasophon-manager/conf:/datasophon-manager/lib/* \
com.datasophon.api.DataSophonApplicationServer