package com.datasophon.plugins.api.model;

/**
 * 命令执行结果
 *
 * @author DataSophon Team
 */
public record CommandResult(String output, String error, int exitCode) {

    public boolean isSuccess() {
        return exitCode == 0;
    }
}