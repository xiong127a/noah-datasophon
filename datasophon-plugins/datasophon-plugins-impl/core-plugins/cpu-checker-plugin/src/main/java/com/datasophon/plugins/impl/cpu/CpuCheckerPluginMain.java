package com.datasophon.plugins.impl.cpu;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * CPU检查器插件主类
 * PF4J插件的主入口类
 * 
 * @author DataSophon Team
 */
@Slf4j
public class CpuCheckerPluginMain extends Plugin {
    
    public CpuCheckerPluginMain(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    public void start() {
        log.info("CPU检查器插件启动: {}", wrapper.getPluginId());
    }
    
    @Override
    public void stop() {
        log.info("CPU检查器插件停止: {}", wrapper.getPluginId());
    }
    
    @Override
    public void delete() {
        log.info("CPU检查器插件删除: {}", wrapper.getPluginId());
    }
}