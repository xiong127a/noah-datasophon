package com.datasophon.worker.strategy.tenantResource;

import com.datasophon.common.Constants;
import com.datasophon.common.model.TenantResource.TenantFrameResource;
import com.datasophon.common.model.TenantResource.TenantHdfsResource;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class HDFSResourceOperateStrategy extends AbstractOperateStrategy implements ResourceOperateStrategy {

    private final TenantHdfsResource hdfsResource;

    public HDFSResourceOperateStrategy(TenantFrameResource tenantResource) {
        super(tenantResource);
        this.hdfsResource = (TenantHdfsResource) tenantResource;
    }

    @Override
    public ExecResult addSource() {
        execResult = createHdfsDir(hdfsResource.getHdfsPath());
        if (execResult.getExecResult()) {
            log.info("hdfs create dir {} success", hdfsResource.getHdfsPath());
        } else {
            log.error("hdfs create dir {} failed", hdfsResource.getHdfsPath());
            log.error(execResult.getExecErrOut());
            return execResult;
        }

        execResult = setHdfsQuota(convertGBToByte(hdfsResource.getHdfsSpaceQuota()), hdfsResource.getHdfsPath());
        if (execResult.getExecResult()) {
            log.info("hdfs set dir {} quota success", hdfsResource.getHdfsPath());
        } else {
            log.error("hdfs set dir {} quota failed", hdfsResource.getHdfsPath());
            log.error(execResult.getExecErrOut());
        }

        return execResult;
    }

    @Override
    public ExecResult updateSource() {
        execResult = setHdfsQuota(convertGBToByte(hdfsResource.getHdfsSpaceQuota()), hdfsResource.getHdfsPath());
        if (execResult.getExecResult()) {
            log.info("hdfs set dir {} quota success", hdfsResource.getHdfsPath());
        } else {
            log.error("hdfs set dir {} quota failed", hdfsResource.getHdfsPath());
            log.error(execResult.getExecErrOut());
        }
        return execResult;
    }

    @Override
    public ExecResult deleteSource() {
        execResult = deleteHdfsPath(hdfsResource.getHdfsPath());
        if (execResult.getExecResult()) {
            log.info("delete hdfs path {} success", hdfsResource.getHdfsPath());
        } else {
            log.error("delete hdfs path {} success", hdfsResource.getHdfsPath());
            log.error(execResult.getExecErrOut());
        }
        return execResult;
    }

    /**
     * 创建hdfs目录
     */
    private ExecResult createHdfsDir(String hdfsPath) {
        List<String> commands = new ArrayList<>();
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfs");
        commands.add("-mkdir");
        commands.add("-p");
        commands.add(hdfsPath);
        return ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 180L, log);
    }

    /**
     * 设置hdfs文件夹容量限额
     */
    private ExecResult setHdfsQuota(String size, String hdfsPath) {
        // /opt/datasophon/hadoop-3.3.3/bin/hdfs dfsadmin -setSpaceQuota 1024 /tenant/t1
        List<String> commands = new ArrayList<>();
        commands.add("sudo");
        commands.add("-u");
        commands.add("hdfs");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfsadmin");
        commands.add("-setSpaceQuota");
        commands.add(size);
        commands.add(hdfsPath);
        return ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 180L, log);
    }


    private ExecResult deleteHdfsPath(String hdfsPath) {
        List<String> commands = new ArrayList<>();
        commands.add("sudo");
        commands.add("-u");
        commands.add("hdfs");
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
        commands.add("dfs");
        commands.add("-rm");
        commands.add("-r");
        commands.add(hdfsPath);
        return ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 180L, log);
    }
}
