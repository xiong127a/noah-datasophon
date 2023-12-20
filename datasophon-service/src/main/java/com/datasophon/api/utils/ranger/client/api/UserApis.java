package com.datasophon.api.utils.ranger.client.api;

import com.datasophon.api.utils.ranger.client.api.feign.UserFeignClient;
import com.datasophon.api.utils.ranger.client.model.User;
import com.datasophon.api.utils.ranger.client.model.Users;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import feign.Param;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class UserApis {

    private final UserFeignClient client;

    public User createUser(final User user) throws RangerClientException {
        return client.createUser(user);
    }

    public Users searchUsers(@Param("stringSearch") final String stringSearch) {
        return client.searchUsers(stringSearch);
    }

    public User getUserByName(@Param("name") String name) throws RangerClientException {
        return client.getUserByName(name);
    }

    public void setUserVisibility(Map<String, Integer> map) {
        client.setUserVisibility(map);
    }
}