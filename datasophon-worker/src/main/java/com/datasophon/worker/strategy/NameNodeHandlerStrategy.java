package com.datasophon.worker.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.JournalNodeClusterUtils;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;
import com.datasophon.worker.utils.KerberosUtils;

import java.nio.file.Paths;
import java.util.List;

public class NameNodeHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    private static final String HADOOP_BIN_DIR = "bin";
    private static final String HDFS_SHELL = "hdfs";
    private static final String RANGER_PLUGIN_DIR = "ranger-hdfs-plugin";
    private static final String SUCCESS_ID_FILE = "success.id";
    private static final long FORMAT_TIMEOUT = 180L;
    private static final int JOURNAL_NODE_CHECK_TIMEOUT_SEC = 30;
    private static final int JOURNAL_NODE_CHECK_MAX_RETRIES = 3;
    private static final int JOURNAL_NODE_CHECK_RETRY_INTERVAL_SEC = 2;

    public NameNodeHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) {
        try {
            String workPath = buildWorkPath(command);
            ServiceHandler serviceHandler = createServiceHandler(command);

            // 处理Kerberos认证
            handleKerberos(command, workPath);

            if (command.getCommandType() == CommandType.INSTALL_SERVICE) {
                // 初始化等待参数
                int totalWaitTime = 60; // 总等待时间（秒）
                int interval = 5;       // 检查间隔（秒）
                int remaining = totalWaitTime;

                logger.info("开始等待JournalNode集群就绪，共需等待{}秒", totalWaitTime);

                while (remaining > 0) {
                    try {
                        // 等待间隔时间
                        Thread.sleep(interval * 1000);

                        // 更新剩余时间并记录日志
                        remaining = Math.max(0, remaining - interval);
                        logger.info("剩余等待时间：{}秒 | 正在检查JournalNode集群状态...", remaining);

                        // 提前检查集群状态
                        if (checkJournalNodeCluster(command,5,1,5)) {
                            logger.info("JournalNode集群已提前就绪，结束等待");
                            break;
                        }
                    } catch (InterruptedException e) {
                        logger.warn("等待过程被中断", e);
                        Thread.currentThread().interrupt();
                        return ExecResult.error("操作被中断");
                    }
                }

                // 最终检查
                if (!checkJournalNodeCluster(command)) {
                    return ExecResult.error(String.format("JournalNode集群在%s秒后仍不可用",totalWaitTime));
                }



                // 处理NameNode初始化
                ExecResult initResult = handleNameNodeInitialization(command, workPath);
                if (!initResult.getExecResult()) {
                    return initResult;
                }
            }

            // 处理Ranger插件
            if (command.getEnableRangerPlugin()) {
                ExecResult rangerResult = enableRangerPlugin(workPath);
                if (!rangerResult.getExecResult()) {
                    return rangerResult;
                }
            }

            // 启动服务
            return startService(serviceHandler, command);
        } catch (Exception e) {
            logger.error("NameNode handler error", e);
            return ExecResult.error(e.getMessage());
        }
    }

    private String buildWorkPath(ServiceRoleOperateCommand command) {
        return Paths.get(Constants.INSTALL_PATH, command.getDecompressPackageName()).toString();
    }

    private ServiceHandler createServiceHandler(ServiceRoleOperateCommand command) {
        return new ServiceHandler(command.getServiceName(), command.getServiceRoleName());
    }

    private void handleKerberos(ServiceRoleOperateCommand command, String workPath) {
        if (command.getEnableKerberos()) {
            logger.info("Initializing Kerberos configuration");
            KerberosUtils.createKeytabDir();

            String hostname = CacheUtils.getString(Constants.HOSTNAME);
            downloadKeytabIfMissing("nn/" + hostname, "nn.service.keytab");
            downloadKeytabIfMissing("HTTP/" + hostname, "spnego.service.keytab");
        }
    }

    private void downloadKeytabIfMissing(String principal, String keytabName) {
        String keytabPath = Paths.get("/etc/security/keytab", keytabName).toString();
        if (!FileUtil.exist(keytabPath)) {
            KerberosUtils.downloadKeytabFromMaster(principal, keytabName);
        }
    }

    private boolean checkJournalNodeCluster(ServiceRoleOperateCommand command,int timeoutSec,
                                            int maxRetries,
                                            int retryIntervalSec) {
        List<String> journalNodeUrls = JournalNodeClusterUtils.buildJournalNodeUrls(
                command.getExtendConfig(), 8485);
        return JournalNodeClusterUtils.checkJournalNodeClusterAvailability(
                journalNodeUrls,
                timeoutSec,
                maxRetries,
                retryIntervalSec);
    }

    private boolean checkJournalNodeCluster(ServiceRoleOperateCommand command) {

        return checkJournalNodeCluster(command,
                JOURNAL_NODE_CHECK_TIMEOUT_SEC,
                JOURNAL_NODE_CHECK_MAX_RETRIES,
                JOURNAL_NODE_CHECK_RETRY_INTERVAL_SEC);
    }

    private ExecResult handleNameNodeInitialization(ServiceRoleOperateCommand command, String workPath) {
        if (command.isSlave()) {
            return handleStandbyNode(workPath);
        } else {
            return handlePrimaryNode(workPath);
        }
    }

    private ExecResult handleStandbyNode(String workPath) {
        logger.info("Initializing Standby NameNode");
        String hdfsBinPath = Paths.get(workPath, HADOOP_BIN_DIR, HDFS_SHELL).toString();
        List<String> commands = CollUtil.newArrayList(
                hdfsBinPath,
                "namenode",
                "-bootstrapStandby"
        );
        return executeWithConfirmation(commands, workPath, "Standby initialization");
    }

    private ExecResult handlePrimaryNode(String workPath) {
        logger.info("Formatting Primary NameNode");
//        clearNameNodeMetadata(workPath);

        String hdfsBinPath = Paths.get(workPath, HADOOP_BIN_DIR, HDFS_SHELL).toString();
        List<String> commands = CollUtil.newArrayList(
                hdfsBinPath,
                "namenode",
                "-format",
                "smhadoop"
        );
        return executeWithConfirmation(commands, workPath, "NameNode format");
    }

    private void clearNameNodeMetadata(String workPath) {
        // 实现自动解析hdfs-site.xml获取元数据目录的逻辑
        String metadataDir = parseMetadataDirFromConfig(workPath);
        if (metadataDir != null) {
            logger.info("Clearing NameNode metadata at: {}", metadataDir);
            FileUtil.del(metadataDir);
        }
    }

    private String parseMetadataDirFromConfig(String workPath) {
        // 实现从hdfs-site.xml解析dfs.namenode.name.dir的逻辑
        // 返回类似"/data/dfs/nn/current"的路径
        return null; // 具体实现需要添加XML解析逻辑
    }

    private ExecResult executeWithConfirmation(List<String> commands, String workPath, String operationName) {
        try {
            ExecResult result = ShellUtils.execWithStatus(
                    workPath,
                    commands,
                    FORMAT_TIMEOUT,
                    logger
            );

            if (result.getExecResult()) {
                logger.info("{} succeeded", operationName);
            } else {
                logger.error("{} failed. Error: {}", operationName, result.getExecErrOut());
            }
            return result;
        } catch (Exception e) {
            logger.error("{} error", operationName, e);
            return ExecResult.error(operationName + " error: " + e.getMessage());
        }
    }

    private ExecResult enableRangerPlugin(String workPath) {
        String pluginPath = Paths.get(workPath, RANGER_PLUGIN_DIR).toString();
        String successFlag = Paths.get(pluginPath, SUCCESS_ID_FILE).toString();

        if (FileUtil.exist(successFlag)) {
            logger.info("Ranger plugin already enabled");
            return ExecResult.success();
        }

        logger.info("Enabling Ranger HDFS plugin");
        List<String> commands = CollUtil.newArrayList(
                "sh",
                Paths.get(pluginPath, "enable-hdfs-plugin.sh").toString()
        );

        ExecResult result = ShellUtils.execWithStatus(
                pluginPath,
                commands,
                FORMAT_TIMEOUT,
                logger
        );

        if (result.getExecResult()) {
            FileUtil.writeUtf8String("success", successFlag);
            logger.info("Ranger plugin enabled successfully");
        } else {
            logger.error("Failed to enable Ranger plugin. Error: {}", result.getExecErrOut());
        }
        return result;
    }

    private ExecResult startService(ServiceHandler serviceHandler, ServiceRoleOperateCommand command) {
        return serviceHandler.start(
                command.getStartRunner(),
                command.getStatusRunner(),
                command.getDecompressPackageName(),
                command.getRunAs()
        );
    }
}
