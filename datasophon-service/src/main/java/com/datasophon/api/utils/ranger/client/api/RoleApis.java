package com.datasophon.api.utils.ranger.client.api;

import com.datasophon.api.utils.ranger.client.model.Role;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@AllArgsConstructor
public class RoleApis {


    private RestTemplate restTemplate;

    private String baseUrl;

    public void createRole(final Role role) throws RangerClientException {
        try {
            String url = baseUrl + "/service/public/v2/api/roles";
            restTemplate.postForObject(url, role, Role.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to create role: {}. Error: {}", role, e.getMessage(), e);
            throw new RangerClientException("Failed to create role: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while creating role: {}. Error: {}", role, e.getMessage(), e);
            throw new RangerClientException("Unexpected error occurred while creating role: " + e.getMessage(), e);
        }
    }

    public Role getRoleByName(final String name) {
        try {
            String url = baseUrl + "/service/public/v2/api/roles/name/" + name;
            return restTemplate.getForObject(url, Role.class);
        } catch (HttpClientErrorException e) {
            log.warn("Failed to get role by name: {}. Error: {}", name, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error occurred while getting role by name: {}. Error: {}", name, e.getMessage(), e);
            return null;
        }
    }

    public void deleteRoleByName(final String name) throws RangerClientException {
        try {
            String url = baseUrl + "/service/public/v2/api/roles/name/" + name;
            restTemplate.delete(url);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete role by name: {}. Error: {}", name, e.getMessage(), e);
            throw new RangerClientException("Failed to delete role by name: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while deleting role by name: {}. Error: {}", name, e.getMessage(), e);
            throw new RangerClientException("Unexpected error occurred while deleting role by name: " + e.getMessage(),
                    e);
        }
    }

    public void addUserAndGroups(final Long id, final Role role) throws RangerClientException {
        try {
            String url = baseUrl + "/service/roles/roles/" + id;
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(role), Role.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to add user and groups to role: {}. Error: {}", role, e.getMessage(), e);
            throw new RangerClientException("Failed to add user and groups to role: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while adding user and groups to role: {}. Error: {}", role,
                    e.getMessage(), e);
            throw new RangerClientException(
                    "Unexpected error occurred while adding user and groups to role: " + e.getMessage(), e);
        }
    }
}
