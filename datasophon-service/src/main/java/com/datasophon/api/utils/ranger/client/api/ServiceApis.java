package com.datasophon.api.utils.ranger.client.api;

import com.datasophon.api.utils.ranger.client.model.Service;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class ServiceApis {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public Service createService(final Service service) throws RangerClientException {
        try {
            String url = baseUrl + "/service/public/v2/api/service";
            return restTemplate.postForObject(url, service, Service.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to create service: {}. Error: {}", service, e.getMessage(), e);
            throw new RangerClientException("Failed to create service: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while creating service: {}. Error: {}", service, e.getMessage(), e);
            throw new RangerClientException("Unexpected error occurred while creating service: " + e.getMessage(), e);
        }
    }

    public Service updateService(final String serviceName, final Service service) throws RangerClientException {
        try {
            String url = baseUrl + "/service/public/v2/api/service/name/" + serviceName;
            return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(service), Service.class).getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to update service: {}. Error: {}", service, e.getMessage(), e);
            throw new RangerClientException("Failed to update service: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while updating service: {}. Error: {}", service, e.getMessage(), e);
            throw new RangerClientException("Unexpected error occurred while updating service: " + e.getMessage(), e);
        }
    }

    public List<Service> searchServices(final String stringSearch) throws RangerClientException {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/service/public/v2/api/service")
                    .queryParam("serviceNamePartial", stringSearch)
                    .build()
                    .toUriString();
            ResponseEntity<List<Service>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Service>>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to search services with query: {}. Error: {}", stringSearch, e.getMessage(), e);
            throw new RangerClientException("Failed to search services: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while searching services: {}. Error: {}", stringSearch, e.getMessage(),
                    e);
            return Collections.emptyList();
        }
    }

    public Service getServiceByName(final String name) {
        try {
            String url = baseUrl + "/service/public/v2/api/service/name/" + name;
            return restTemplate.getForObject(url, Service.class);
        } catch (HttpClientErrorException e) {
            log.warn("Failed to get service by name: {}. Error: {}", name, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error occurred while getting service by name: {}. Error: {}", name, e.getMessage(),
                    e);
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }
}
