package com.datasophon.api.utils.ranger.client.api.feign;

import com.datasophon.api.utils.ranger.client.model.Role;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import feign.Param;
import feign.RequestLine;

public interface RoleFeignClient {

    @RequestLine("POST /service/public/v2/api/roles")
    Role createRole(final Role role) throws RangerClientException;

    @RequestLine("GET /service/public/v2/api/roles/name/{name}")
    Role getRoleByName(@Param("name") final String name);

    @RequestLine("DELETE /service/public/v2/api/roles/name/{name}")
    void deleteRoleByName(@Param("name") final String name);

    @RequestLine("PUT /service/roles/roles/{id}")
    Role addUserAndGroups(@Param("id") final Integer id, final Role role);

}
