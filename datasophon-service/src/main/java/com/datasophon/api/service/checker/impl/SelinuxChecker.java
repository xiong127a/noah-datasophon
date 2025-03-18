package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SelinuxChecker extends AbstractItemChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(SelinuxChecker.class);
    
    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.debug("======== SELinux检查开始 ========");
            cacheLog.debug("检查主机: %s, 检查项ID: %d", hostInfo.getHostname(), checkItem.getId());
            cacheLog.info("==== 开始检查SELinux状态 ====");
            cacheLog.info("主机: %s", hostInfo.getHostname());
            
            // 检查SELinux状态
            cacheLog.debug("准备执行getenforce命令检查SELinux状态");
            cacheLog.info("执行getenforce命令检查SELinux状态...");
            String selinuxStatus = execCommand(session, "getenforce");
            cacheLog.debug("getenforce命令返回结果: '%s'", selinuxStatus.trim());
            cacheLog.info("SELinux状态: %s", selinuxStatus.trim());
            
            // 详细记录SELinux状态检查逻辑
            if (selinuxStatus.contains("Disabled")) {
                cacheLog.debug("SELinux状态为完全禁用(Disabled)");
            } else if (selinuxStatus.contains("Permissive")) {
                cacheLog.debug("SELinux状态为宽容模式(Permissive)");
            } else if (selinuxStatus.contains("Enforcing")) {
                cacheLog.debug("SELinux状态为强制模式(Enforcing)");
            } else {
                cacheLog.debug("SELinux状态未知或getenforce命令未正确返回: '%s'", selinuxStatus);
            }
            
            if (selinuxStatus.contains("Disabled") || selinuxStatus.contains("Permissive")) {
                cacheLog.debug("SELinux检查判定结果: 通过 (状态为Disabled或Permissive)");
                cacheLog.info("SELinux检查通过: 已禁用或处于宽容模式");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage("SELinux已禁用或处于宽容模式");
            } else if (selinuxStatus.contains("Enforcing")) {
                cacheLog.debug("SELinux检查判定结果: 未通过 (状态为Enforcing)");
                cacheLog.warn("SELinux检查未通过: 处于强制模式，建议禁用");
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("SELinux处于强制模式，建议禁用");
            } else {
                cacheLog.debug("getenforce命令结果不明确，尝试检查配置文件");
                cacheLog.info("getenforce命令结果不明确，尝试检查配置文件...");
                
                // 检查配置文件
                cacheLog.debug("查找/etc/selinux/config文件中的SELINUX=disabled配置");
                String configCheck = execCommand(session, "grep -i 'SELINUX=' /etc/selinux/config | grep -i 'disabled'");
                cacheLog.debug("配置文件检查命令返回: '%s'", configCheck);
                cacheLog.info("配置文件检查结果: %s", configCheck.isEmpty() ? "未禁用" : "已禁用");
                
                if (!configCheck.isEmpty()) {
                    cacheLog.debug("SELinux配置文件检查通过: 找到SELINUX=disabled配置");
                    cacheLog.info("SELinux检查通过: 配置文件中已设置为禁用");
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage("SELinux配置为禁用状态");
                } else {
                    cacheLog.debug("SELinux配置文件检查未通过: 未找到SELINUX=disabled配置");
                    cacheLog.warn("SELinux检查未通过: 配置文件中未禁用");
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("SELinux未禁用");
                }
            }
            
            cacheLog.debug("SELinux检查最终结果: %s", checkItem.getStatus());
            cacheLog.debug("SELinux检查消息: %s", checkItem.getMessage());
            cacheLog.info("==== SELinux检查完成 ====");
            cacheLog.debug("======== SELinux检查结束 ========");
        } catch (Exception e) {
            cacheLog.debug("SELinux检查过程中发生异常: %s", e.getMessage());
            cacheLog.debug("异常堆栈: %s", e.toString());
            logger.error("SELinux检查失败: {}", e.getMessage());
            cacheLog.error("SELinux检查失败: %s", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("SELinux检查失败: " + e.getMessage());
        }
        return checkItem;
    }
    
    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.debug("======== SELinux修复开始 ========");
            cacheLog.debug("修复主机: %s, 检查项ID: %d", hostInfo.getHostname(), checkItem.getId());
            cacheLog.info("==== 开始修复SELinux配置 ====");
            cacheLog.info("主机: %s", hostInfo.getHostname());
            
            // 临时设置SELinux为宽容模式
            cacheLog.debug("步骤1: 尝试临时设置SELinux为宽容模式");
            cacheLog.info("设置SELinux为宽容模式 (临时)...");
            
            String setenforceResult = execCommand(session, "setenforce 0");
            cacheLog.debug("setenforce 0命令返回: '%s'", setenforceResult);
            
            if (setenforceResult.startsWith("ERROR")) {
                cacheLog.debug("临时设置SELinux失败: %s", setenforceResult);
                cacheLog.warn("临时设置SELinux为宽容模式可能失败: %s", setenforceResult);
            } else {
                cacheLog.debug("临时设置SELinux为宽容模式成功");
                cacheLog.info("已临时设置SELinux为宽容模式");
            }
            
            // 永久禁用SELinux
            cacheLog.debug("步骤2: 永久修改SELinux配置");
            cacheLog.info("修改配置文件，永久禁用SELinux...");
            
            // 先检查文件是否存在
            cacheLog.debug("检查/etc/selinux/config文件是否存在");
            String checkFileResult = execCommand(session, "[ -f /etc/selinux/config ] && echo 'exists' || echo 'not_exists'");
            cacheLog.debug("配置文件检查结果: %s", checkFileResult.trim());
            
            if ("not_exists".equals(checkFileResult.trim())) {
                cacheLog.debug("SELinux配置文件不存在，可能系统未安装SELinux");
                cacheLog.info("未找到SELinux配置文件，系统可能未安装SELinux");
                return true; // 如果没有SELinux，则视为已成功禁用
            }
            
            // 修改配置文件
            cacheLog.debug("使用sed命令修改/etc/selinux/config文件");
            String sedResult = execCommand(session, "sed -i 's/^SELINUX=.*/SELINUX=disabled/g' /etc/selinux/config");
            cacheLog.debug("sed命令返回: '%s'", sedResult);
            
            if (sedResult.startsWith("ERROR")) {
                cacheLog.debug("永久禁用SELinux失败: %s", sedResult);
                cacheLog.error("永久禁用SELinux失败: %s", sedResult);
                return false;
            }
            
            // 验证修改
            cacheLog.debug("步骤3: 验证配置文件修改结果");
            cacheLog.info("验证配置文件修改结果...");
            
            String verifyResult = execCommand(session, "grep -i 'SELINUX=' /etc/selinux/config");
            cacheLog.debug("配置文件验证结果: '%s'", verifyResult.trim());
            cacheLog.info("修改后的配置: %s", verifyResult.trim());
            
            boolean configVerified = verifyResult.toLowerCase().contains("disabled");
            cacheLog.debug("配置验证结果: %s", configVerified ? "成功" : "失败");
            
            if (configVerified) {
                cacheLog.debug("SELinux已成功配置为禁用状态");
                cacheLog.info("SELinux配置已成功修改为禁用状态");
                cacheLog.info("==== SELinux配置修复完成 ====");
                cacheLog.debug("======== SELinux修复结束 ========");
                return true;
            } else {
                cacheLog.debug("SELinux配置修改验证失败，配置文件中未包含disabled设置");
                cacheLog.error("SELinux配置修改失败，未包含disabled设置");
                cacheLog.debug("======== SELinux修复失败 ========");
                return false;
            }
        } catch (Exception e) {
            cacheLog.debug("SELinux修复过程中发生异常: %s", e.getMessage());
            cacheLog.debug("异常堆栈: %s", e.toString());
            logger.error("SELinux配置修复失败: {}", e.getMessage());
            cacheLog.error("SELinux配置修复失败: %s", e.getMessage());
            cacheLog.debug("======== SELinux修复异常结束 ========");
            return false;
        }
    }
    
    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.SELINUX;
    }
} 