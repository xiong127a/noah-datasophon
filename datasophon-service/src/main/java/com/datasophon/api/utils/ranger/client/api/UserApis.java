package com.datasophon.api.utils.ranger.client.api;

import com.datasophon.api.utils.ranger.client.model.User;
import com.datasophon.api.utils.ranger.client.model.Users;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class UserApis {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public void createUser(final User user) throws RangerClientException {
        try {
            String url = baseUrl + "/service/xusers/secure/users";
            restTemplate.postForObject(url, user, User.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to create user: {}. Error: {}", user, e.getMessage(), e);
            throw new RangerClientException("Failed to create user: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while creating user: {}. Error: {}", user, e.getMessage(), e);
            throw new RangerClientException("Unexpected error occurred while creating user: " + e.getMessage(), e);
        }
    }

    public Users searchUsers(final String stringSearch) {
        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl + "/service/xusers/users")
                    .queryParam("name", stringSearch)
                    .build()
                    .toUriString();
            return restTemplate.getForObject(url, Users.class);
        } catch (Exception e) {
            log.error("Failed to search users with query: {}. Error: {}", stringSearch, e.getMessage(), e);
            return new Users();
        }
    }

    public User getUserByName(String name) {
        try {
            String url = baseUrl + "/service/xusers/users/userName/" + name;
            return restTemplate.getForObject(url, User.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to get user by name: {}. Error: {}", name, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error occurred while getting user by name: {}. Error: {}", name, e.getMessage(), e);
            return null;
        }
    }

    public void setUserVisibility(Map<String, Integer> map) {
        try {
            String url = baseUrl + "/service/xusers/secure/users/visibility";
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(map), Void.class);
        } catch (Exception e) {
            log.error("Failed to set user visibility: {}. Error: {}", map, e.getMessage(), e);
        }
    }
}