package com.datasophon.api.service.checker.impl.firewall;

/**
 * 防火墙检查结果
 */
public class FirewallCheckResult {
    
    /**
     * 防火墙是否开启
     */
    private boolean enabled;
    
    /**
     * 执行命令是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 原始输出
     */
    private String rawOutput;
    
    /**
     * 退出码
     */
    private int exitCode;
    
    public FirewallCheckResult() {
        this.success = false;
        this.enabled = false;
        this.errorMessage = "";
        this.rawOutput = "";
        this.exitCode = -1;
    }
    
    /**
     * 创建表示执行成功的结果
     */
    public static FirewallCheckResult success(boolean enabled, String output, int exitCode) {
        FirewallCheckResult result = new FirewallCheckResult();
        result.setSuccess(true);
        result.setEnabled(enabled);
        result.setRawOutput(output);
        result.setExitCode(exitCode);
        return result;
    }
    
    /**
     * 创建表示执行失败的结果
     */
    public static FirewallCheckResult failure(String errorMessage, String output, int exitCode) {
        FirewallCheckResult result = new FirewallCheckResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setRawOutput(output);
        result.setExitCode(exitCode);
        return result;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getRawOutput() {
        return rawOutput;
    }
    
    public void setRawOutput(String rawOutput) {
        this.rawOutput = rawOutput;
    }
    
    public int getExitCode() {
        return exitCode;
    }
    
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "防火墙状态: " + (enabled ? "已启用" : "已禁用") + ", 退出码: " + exitCode;
        } else {
            return "检查失败: " + errorMessage + ", 退出码: " + exitCode;
        }
    }
} 