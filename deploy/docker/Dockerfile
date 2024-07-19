FROM openjdk:8
RUN mkdir /datasophon
COPY ./bin/ /datasophon/bin/
COPY ./conf/ /datasophon/conf/
COPY ./lib/ /datasophon/lib/
COPY ./jmx/ /datasophon/jmx/
ENV TZ=Asia/Shanghai
EXPOSE 8081
EXPOSE 8586
EXPOSE 2551
WORKDIR /datasophon
ENTRYPOINT java -server -Xms1G -Xmx1G -Xmn512m \
-Dspring.config.location=/datasophon/conf/application.yml \
-Dfile.encoding=UTF-8 \
-javaagent:/datasophon/jmx/jmx_prometheus_javaagent-0.16.1.jar=8586:/datasophon/jmx/jmx_exporter_config.yaml \
-classpath /datasophon/conf:/datasophon/lib/* \
com.datasophon.api.DataSophonApplicationServer