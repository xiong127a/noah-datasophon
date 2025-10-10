package com.datasophon.api.utils.ranger.client.api;

import com.datasophon.api.utils.ranger.client.api.feign.ServiceFeignClient;
import com.datasophon.api.utils.ranger.client.model.Service;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import feign.Param;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class ServiceApis {

    private final ServiceFeignClient client;

    public Service createService(final Service service) throws RangerClientException {
        return client.createService(service);
    }

    public Service updateService(@Param("serviceName") final String serviceName,
                                 final Service service) throws RangerClientException {
        return client.updateService(serviceName, service);
    }

    public List<Service> searchServices(@Param("stringSearch") final String stringSearch) throws RangerClientException {
        return client.searchServices(stringSearch);
    }

    public Service getServiceByName(@Param("name") final String name) {
        try {
            return client.getServiceByName(name);
        } catch (RangerClientException e) {
            log.warn("Failed to get service by name: {}. Error: {}", name, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error occurred while getting service by name: {}. Error: {}", name, e.getMessage(), e);
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }
}
