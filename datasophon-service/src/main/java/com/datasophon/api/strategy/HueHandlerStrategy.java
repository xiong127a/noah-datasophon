package com.datasophon.api.strategy;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import cn.hutool.core.convert.Convert;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ServiceConfig;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class HueHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {


    @Override
    public void handlerConfig(Long clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        for (ServiceConfig serviceConfig : list) {
            if ("enableHueKerberos".equals(serviceConfig.getName())) {
                if (Convert.toBool(serviceConfig.getValue())) {
                    simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${enableHUEKerberos}", "true");
                } else {
                    simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${enableHUEKerberos}", "false");
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
                "pekko://datasophon@" + nnHost + ":2552/user/worker/executeShellActor");
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



    public String kinitKbStr(String user) {
        return "kinit -kt /etc/security/keytab/" + user + ".service.keytab " + user + "/" + Convert.toStr(CacheUtils.get(Constants.HOSTNAME)) + "@HADOOP.COM";
    }
}
