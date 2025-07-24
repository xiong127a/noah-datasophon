package com.datasophon.api.utils.ranger.client;

import com.datasophon.api.utils.ranger.client.api.PolicyApis;
import com.datasophon.api.utils.ranger.client.api.RoleApis;
import com.datasophon.api.utils.ranger.client.api.ServiceApis;
import com.datasophon.api.utils.ranger.client.api.UserApis;
import com.datasophon.api.utils.ranger.client.config.RangerClientConfig;
import com.datasophon.api.utils.ranger.client.utils.ClientException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class RangerClient implements Client {

    @Getter
    private UserApis users;

    @Getter
    private ServiceApis services;

    @Getter
    private PolicyApis policies;

    @Getter
    private RoleApis roles;

    private final RangerClientConfig clientConfig;
    private RestTemplate restTemplate;

    public RangerClient(RangerClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    private final static ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void start() {
        if (started.get()) {
            log.info("apache-ranger client is already started...");
            return;
        }

        initialize();

        this.started.set(true);
        log.info("Initialized apache-ranger client...");
    }

    private void initialize() {
        // 创建并配置RestTemplate
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(mapper);

        // 添加Basic认证拦截器
        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            String auth = clientConfig.getAuthConfig().getUsername() + ":" + clientConfig.getAuthConfig().getPassword();
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            request.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            return execution.execute(request, body);
        };

        // 创建一个简单的请求工厂并设置超时
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(clientConfig.getConnectTimeoutMillis());
        factory.setReadTimeout(clientConfig.getReadTimeoutMillis());

        // 构建RestTemplate
        this.restTemplate = new RestTemplateBuilder()
                .requestFactory(() -> factory)
                .additionalMessageConverters(messageConverter)
                .additionalInterceptors(authInterceptor)
                .build();

        // 初始化API接口
        String baseUrl = clientConfig.getUrl();
        users = new UserApis(restTemplate, baseUrl);
        services = new ServiceApis(restTemplate, baseUrl);
        policies = new PolicyApis(restTemplate, baseUrl);
        roles = new RoleApis(restTemplate, baseUrl);
    }

    @Override
    public void stop() {
        isStarted();
        this.started.set(false);
        log.info("Stopped apache-ranger client...");
    }

    /*
     * Is client Started?
     */
    private void isStarted() {
        if (!this.started.get()) {
            log.error("apache-ranger client is not yet started.");
            throw new ClientException("apache-ranger client is not yet started.");
        }
    }
}
