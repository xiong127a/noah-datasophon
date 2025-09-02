package com.datasophon.plugins.impl.ssh;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * SSH连接器插件主类
 * PF4J插件的主入口类，集成Spring支持
 * 
 * @author DataSophon Team
 */
@Slf4j
public class SshConnectorPluginMain extends SpringPlugin {
    
    public SshConnectorPluginMain(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    protected ApplicationContext createApplicationContext() {
        log.info("创建SSH连接器插件Spring上下文");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        // 扫描插件包路径
        applicationContext.scan("com.datasophon.plugins.impl.ssh", "com.datasophon.plugins.ssh");
        applicationContext.refresh();
        log.info("SSH连接器插件Spring上下文创建完成");
        return applicationContext;
    }
    
    @Override
    public void start() {
        log.info("SSH连接器插件启动: {}", wrapper.getPluginId());
        super.start(); // 调用SpringPlugin的start方法
    }
    
    @Override
    public void stop() {
        log.info("SSH连接器插件停止: {}", wrapper.getPluginId());
        super.stop(); // 调用SpringPlugin的stop方法
    }
    
    @Override
    public void delete() {
        log.info("SSH连接器插件删除: {}", wrapper.getPluginId());
        super.delete(); // 调用SpringPlugin的delete方法
    }
}