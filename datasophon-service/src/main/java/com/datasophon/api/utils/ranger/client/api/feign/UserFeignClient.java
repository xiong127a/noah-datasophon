package com.datasophon.api.utils.ranger.client.api.feign;

import com.datasophon.api.utils.ranger.client.model.User;
import com.datasophon.api.utils.ranger.client.model.Users;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import feign.Param;
import feign.RequestLine;

import java.util.Map;

public interface UserFeignClient {

    /*
    Mapper to Ranger User APIs
     */
    @RequestLine("POST /service/xusers/secure/users")
    User createUser(final User user) throws RangerClientException;

    @RequestLine("GET /service/xusers/users?name={stringSearch}")
    Users searchUsers(@Param("stringSearch") final String stringSearch);

    @RequestLine("GET /service/xusers/users/userName/{name}")
    User getUserByName(@Param("name") String name) throws RangerClientException;

    @RequestLine("PUT /service/xusers/secure/users/visibility")
    void setUserVisibility(Map<String, Integer> map);
}
