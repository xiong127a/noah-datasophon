package com.datasophon.api.strategy;

import cn.hutool.core.convert.Convert;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.handler.service.WorkerTaskHelper;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.ServiceConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        // 使用HTTP方式提交命令到Worker
        ExecuteCmdCommand command = new ExecuteCmdCommand();
        
        ArrayList<String> commands = new ArrayList<>();
        commands.add("sudo");
        commands.add("-u");
        commands.add("hdfs");
        commands.add(kinitKbStr("nn"));
        commands.add("&&");
        commands.add("sudo");
        commands.add("-u");
        commands.add("hdfs");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfs");
        commands.add("-mkdir");
        commands.add("-p");
        commands.add("/user/HTTP");
        commands.add("&&");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfs");
        commands.add("-chmod");
        commands.add("-R");
        commands.add("777");
        commands.add("/user/HTTP");
        
        command.setCommands(commands);
        
        // 异步提交，不等待结果
        WorkerTaskHelper.submitAsync(nnHost, command);
    }



    public String kinitKbStr(String user) {
        return "kinit -kt /etc/security/keytab/" + user + ".service.keytab " + user + "/" + Convert.toStr(CacheUtils.get(Constants.HOSTNAME)) + "@HADOOP.COM";
    }
}
