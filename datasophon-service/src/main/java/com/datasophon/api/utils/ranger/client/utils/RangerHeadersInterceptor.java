package com.datasophon.api.utils.ranger.client.utils;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class RangerHeadersInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().set("Accept", "application/json");
        request.getHeaders().set("X-XSRF-HEADER", "\"\"");
        request.getHeaders().set("Content-Type", "application/json");
        return execution.execute(request, body);
    }
}
