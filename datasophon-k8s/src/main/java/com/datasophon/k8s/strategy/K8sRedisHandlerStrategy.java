package com.datasophon.k8s.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sUtil;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.sql.SQLException;
import java.util.ArrayList;

public class K8sRedisHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public K8sRedisHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        ExecResult startResult;
        String hostname = command.getHostname();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            /*if (serviceRoleName.equals("RedisMaster")&&!K8sMinaUtils.checkPathExists(hostname,workPath+"/cluster/dump-master.rdb")){
                K8sMinaUtils.createFile(hostname,workPath+"/cluster/dump-master.rdb");
            }
            if (serviceRoleName.equals("RedisWorker")&&!K8sMinaUtils.checkPathExists(hostname,workPath+"/cluster/dump-slave.rdb")){
                K8sMinaUtils.createFile(hostname,workPath+"/cluster/dump-slave.rdb");
            }*/

            startResult = serviceHandler.start(command);

            ArrayList<String> commands = new ArrayList<>();
            commands.add("chmod");
            commands.add("+x");
            commands.add(workPath + "/redis-cluster.sh");
            commands.add("&&");
            commands.add("sh");
            commands.add(workPath + "/redis-cluster.sh");
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                K8sUtil.runCmd(
                        Constants.DATASOPHON,
                        kubeClient,
                        (command.getServiceName()+"-"+command.getServiceRoleName()).toLowerCase(),
                        command.getHostname(),
                        String.join(" ",commands));
                logger.info("sh redis-cluster.sh success");
                startResult.setExecResult(true);
            } catch (Exception e) {
                logger.info("sh redis-cluster.sh failed");
                startResult.setExecResult(false);
                return startResult;
            }
            return startResult;
        }
        startResult = serviceHandler.start(command);
        return startResult;
    }
}
