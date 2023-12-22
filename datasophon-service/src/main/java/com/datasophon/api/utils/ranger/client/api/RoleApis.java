package com.datasophon.api.utils.ranger.client.api;

import com.datasophon.api.utils.ranger.client.api.feign.RoleFeignClient;
import com.datasophon.api.utils.ranger.client.model.Role;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import feign.Param;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RoleApis {

    private final RoleFeignClient client;

    public Role createRole(final Role role) throws RangerClientException {
        return client.createRole(role);
    }

    public Role getRoleByName(@Param("name") final String name) throws RangerClientException {
        return client.getRoleByName(name);
    }

    public void deleteRoleByName(@Param("name") final String name) throws RangerClientException {
        client.deleteRoleByName(name);
    }

    public Role addUserAndGroups(@Param("id") final Integer id, final Role role) throws RangerClientException {
        return client.addUserAndGroups(id, role);
    }
}
