package com.datasophon.api.utils.ranger.client.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RangerClientConfig {

    private int connectTimeoutMillis = 5 * 1000;
    private int readTimeoutMillis = 30 * 1000;

    // 日志级别，可以使用Spring Boot自带的日志配置
    private String loggingLevel = "INFO";

    private String url = "http://localhost:6080";

    private RangerAuthConfig authConfig = new RangerAuthConfig();

    @Override
    public String toString() {
        return "RangerClientConfig{" +
                "connectTimeoutMillis=" + connectTimeoutMillis +
                ", readTimeoutMillis=" + readTimeoutMillis +
                ", loggingLevel='" + loggingLevel + '\'' +
                ", url='" + url + '\'' +
                ", authConfig=" + authConfig +
                '}';
    }
}
