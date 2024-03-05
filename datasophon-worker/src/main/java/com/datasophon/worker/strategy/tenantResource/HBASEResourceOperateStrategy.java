package com.datasophon.worker.strategy.tenantResource;

import com.datasophon.common.Constants;
import com.datasophon.common.model.TenantResource.TenantFrameResource;
import com.datasophon.common.model.TenantResource.TenantHbaseResource;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HBASEResourceOperateStrategy extends AbstractOperateStrategy implements ResourceOperateStrategy {

    private final TenantHbaseResource hbaseResource;

    public HBASEResourceOperateStrategy(TenantFrameResource tenantResource) {
        super(tenantResource);
        this.hbaseResource = (TenantHbaseResource) tenantResource;
    }

    @Override
    public ExecResult addSource() {
        execResult = createHbaseNamespace(hbaseResource.getHbaseNamespace(), hbaseResource.getHbaseCapacity());
        if (execResult.getExecResult()) {
            log.info("create hbase namespace {} success", hbaseResource.getHbaseNamespace());
        } else {
            log.error("create hbase namespace {} failed", hbaseResource.getHbaseNamespace());
            log.error(execResult.getExecErrOut());
        }
        return execResult;
    }

    @Override
    public ExecResult updateSource() {
        execResult = alertHbaseNamespace(hbaseResource);
        if (execResult.getExecResult()) {
            log.info("alter hbase namespace {} quota success", hbaseResource.getHbaseNamespace());
        } else {
            log.error("alter hbase namespace {} quota failed", hbaseResource.getHbaseNamespace());
            log.error(execResult.getExecErrOut());
        }
        return execResult;
    }

    @Override
    public ExecResult deleteSource() {
        execResult = dropHbaseNamespace(hbaseResource.getHbaseNamespace());
        if (execResult.getExecResult()) {
            log.info("drop hbase namespace {} success", hbaseResource.getHbaseNamespace());
        } else {
            log.error("drop hbase namespace {} failed", hbaseResource.getHbaseNamespace());
            log.error(execResult.getExecErrOut());
        }
        return execResult;
    }

    private ExecResult alertHbaseNamespace(TenantHbaseResource hbaseResource) {
        ExecResult execResult;
        // echo "alter_namespace 'test4'; set_quota TYPE => SPACE, NAMESPACE => 'test4', LIMIT => '1G', POLICY => NO_INSERTS; " | hbase shell
        execResult = ShellUtils.exceShell(
                "echo \"alter_namespace '" +
                        hbaseResource.getHbaseNamespace() +
                        "'; " +
                        "set_quota TYPE => SPACE, NAMESPACE => '" +
                        hbaseResource.getHbaseNamespace() +
                        "', " +
                        "LIMIT => '" +
                        hbaseResource.getHbaseCapacity() +
                        "G', POLICY => NO_INSERTS;\" | " +
                        Constants.INSTALL_PATH +
                        "/hbase-2.4.16/bin/hbase shell");
        return execResult;
    }

    private ExecResult createHbaseNamespace(String hbaseNamespace, String hbaseCapacity) {
        // echo "create_namespace 'test4'; set_quota TYPE => SPACE, NAMESPACE => 'test4', LIMIT => '1G', POLICY => NO_INSERTS; " | hbase shell
        return ShellUtils.exceShell(
                "echo \"create_namespace '" +
                        hbaseNamespace +
                        "'; " +
                        "set_quota TYPE => SPACE, NAMESPACE => '" +
                        hbaseNamespace +
                        "', " +
                        "LIMIT => '" +
                        hbaseCapacity +
                        "G', POLICY => NO_INSERTS;\" | " +
                        Constants.INSTALL_PATH +
                        "/hbase-2.4.16/bin/hbase shell");
    }

    private ExecResult dropHbaseNamespace(String hbaseNamespace) {
        // echo "drop_namespace 'test4';" | hbase shell
        return ShellUtils.exceShell(
                "echo \"drop_namespace '" +
                        hbaseNamespace +
                        "';\" | " +
                        Constants.INSTALL_PATH +
                        "/hbase-2.4.16/bin/hbase shell");
    }
}