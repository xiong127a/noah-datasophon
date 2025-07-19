package com.datasophon.api.utils.ranger.client.utils;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class RangerErrorDecoder extends DefaultResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        try {
            String statusText = response.getStatusText();
            int statusCode = response.getStatusCode().value();
            String responseBody = readResponseBodyAsString(response);

            throw new RangerClientException(
                    statusCode,
                    String.format("Status %s (%d); content: %s",
                            statusText,
                            statusCode,
                            responseBody));
        } catch (IOException e) {
            throw new RestClientException("Error reading response", e);
        }
    }

    private String readResponseBodyAsString(ClientHttpResponse response) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "[Could not read response body]";
        }
    }
}
