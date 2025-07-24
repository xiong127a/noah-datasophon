package com.datasophon.api.utils.ranger.client.utils;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RangerAuthHeadersInterceptor implements ClientHttpRequestInterceptor {


    private String username;

    private String password;



    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().set("Accept", "application/json");
        request.getHeaders().set("Content-Type", "application/json");
        request.getHeaders().set("Authorization", getAuthorization());
        return execution.execute(request, body);
    }

    private String getAuthorization() {
        byte[] auth = (username + ":" + password).getBytes(StandardCharsets.UTF_8);
        String encoded = Base64.getEncoder().encodeToString(auth);
        return "Basic " + encoded;
    }
}
