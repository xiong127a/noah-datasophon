package com.datasophon.api.strategy;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.convert.Convert;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

public class HueHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    private static final Logger logger = LoggerFactory.getLogger(HueHandlerStrategy.class);

    @Override
    public void handler(Integer clusterId, List<String> hosts) {

    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        for (ServiceConfig serviceConfig : list) {
            if ("enableHueKerberos".equals(serviceConfig.getName())) {
                if (Convert.toBool(serviceConfig.getValue())) {
                    ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enableHUEKerberos}", "true");
                } else {
                    ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enableHUEKerberos}", "false");
                }
            }
        }
        if ("true".equals(globalVariables.get("${enableHDFSKerberos}"))) {
            String nnHost = globalVariables.get("${nn1}");
            createHdfsDir(nnHost);
        }
    }

    private void createHdfsDir(String nnHost) {
        ActorSelection execCmdActor = ActorUtils.actorSystem.actorSelection(
                "akka.tcp://datasophon@" + nnHost + ":2552/user/worker/executeShellActor");
        StringJoiner commands = new StringJoiner(" ");
        commands.add("sudo");
        commands.add("-u");
        commands.add("hdfs");
        commands.add(kinitKbStr("nn"));
        commands.add(";");
        commands.add("sudo");
        commands.add("-u");
        commands.add("hdfs");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfs");
        commands.add("-mkdir");
        commands.add("-p");
        commands.add("/user/HTTP");
        commands.add(";");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfs");
        commands.add("-chmod");
        commands.add("-R");
        commands.add("777");
        commands.add("/user/HTTP");
        execCmdActor.tell(commands.toString(), ActorRef.noSender());
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {

    }

    public String kinitKbStr(String user) {
        return "kinit -kt /etc/security/keytab/" + user + ".service.keytab " + user + "/" + Convert.toStr(CacheUtils.get(Constants.HOSTNAME)) + "@HADOOP.COM";
    }
}
