package com.datasophon.api.utils.ranger.client.api;

import com.datasophon.api.utils.ranger.client.model.Policy;
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
public class PolicyApis {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public void createPolicy(final Policy policy) throws RangerClientException {
        try {
            String url = baseUrl + "/service/public/v2/api/policy";
            restTemplate.postForObject(url, policy, Policy.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to create policy: {}. Error: {}", policy, e.getMessage(), e);
            throw new RangerClientException("Failed to create policy: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while creating policy: {}. Error: {}", policy, e.getMessage(), e);
            throw new RangerClientException("Unexpected error occurred while creating policy: " + e.getMessage(), e);
        }
    }

    public Policy getPolicyByName(final String serviceName, final String policyName) throws RangerClientException {
        try {
            String url = baseUrl + "/service/public/v2/api/service/" + serviceName + "/policy/" + policyName;
            return restTemplate.getForObject(url, Policy.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to get policy by name: {}/{}. Error: {}", serviceName, policyName, e.getMessage(), e);
            throw new RangerClientException("Failed to get policy by name: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while getting policy by name: {}/{}. Error: {}", serviceName,
                    policyName, e.getMessage(), e);
            throw new RangerClientException("Unexpected error occurred while getting policy by name: " + e.getMessage(),
                    e);
        }
    }

    public void updatePolicy(final int policyId, final Policy policy) throws RangerClientException {
        try {
            String url = baseUrl + "/service/public/v2/api/policy/" + policyId;
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(policy), Policy.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to update policy: {}. Error: {}", policy, e.getMessage(), e);
            throw new RangerClientException("Failed to update policy: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while updating policy: {}. Error: {}", policy, e.getMessage(), e);
            throw new RangerClientException("Unexpected error occurred while updating policy: " + e.getMessage(), e);
        }
    }

    public List<Policy> searchPolicies(final String serviceName, final String stringSearch)
            throws RangerClientException {
        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl + "/service/public/v2/api/policy")
                    .queryParam("serviceName", serviceName)
                    .queryParam("policyNamePartial", stringSearch)
                    .build()
                    .toUriString();
            ResponseEntity<List<Policy>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to search policies with query: {}/{}. Error: {}", serviceName, stringSearch,
                    e.getMessage(), e);
            throw new RangerClientException("Failed to search policies: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while searching policies: {}/{}. Error: {}", serviceName, stringSearch,
                    e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Policy> getAllPoliciesByService(final String serviceName) throws RangerClientException {
        try {
            String url = baseUrl + "/service/public/v2/api/service/" + serviceName + "/policy";
            ResponseEntity<List<Policy>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get all policies for service: {}. Error: {}", serviceName, e.getMessage(), e);
            throw new RangerClientException("Failed to get all policies for service: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while getting all policies for service: {}. Error: {}", serviceName,
                    e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public void deletePolicy(final int policyId) throws RangerClientException {
        try {
            String url = baseUrl + "/service/public/v2/api/policy/" + policyId;
            restTemplate.delete(url);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete policy: {}. Error: {}", policyId, e.getMessage(), e);
            throw new RangerClientException("Failed to delete policy: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while deleting policy: {}. Error: {}", policyId, e.getMessage(), e);
            throw new RangerClientException("Unexpected error occurred while deleting policy: " + e.getMessage(), e);
        }
    }
}
