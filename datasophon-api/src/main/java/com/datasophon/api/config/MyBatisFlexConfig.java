package com.datasophon.api.config;

import com.mybatisflex.core.FlexGlobalConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisFlexConfig {

    @Bean
    public FlexGlobalConfig flexGlobalConfig() {
        FlexGlobalConfig config = new FlexGlobalConfig();
        // 设置打印banner为false
        config.setPrintBanner(false);
        return config;
    }
}