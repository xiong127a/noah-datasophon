package com.datasophon.api.utils.ranger.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Policy {
    Map<String, PolicyResource> resources;
    List<PolicyItem> policyItems = Lists.newArrayList();
    List<PolicyItem> denyPolicyItems = Lists.newArrayList();
    List<PolicyItem> allowExceptions = Lists.newArrayList();
    List<PolicyItem> denyExceptions = Lists.newArrayList();
    List<Object> dataMaskPolicyItems = Lists.newArrayList();
    List<Object> rowFilterPolicyItems = Lists.newArrayList();
    private Integer id;
    private String guid;
    private Boolean isEnabled;
    private Integer version;
    private String service;
    private String name;
    private Integer policyType;
    private String description;
    private Boolean isAuditEnabled;
    private Boolean isDenyAllElse;
    private Integer policyPriority;
    private List<String> policyLabels;
    private String resourceSignature;
    private String serviceType;

    @Override
    public String toString() {
        return "Policy{" +
                "resources=" + resources +
                ", policyItems=" + policyItems +
                ", denyPolicyItems=" + denyPolicyItems +
                ", allowExceptions=" + allowExceptions +
                ", denyExceptions=" + denyExceptions +
                ", dataMaskPolicyItems=" + dataMaskPolicyItems +
                ", rowFilterPolicyItems=" + rowFilterPolicyItems +
                ", id=" + id +
                ", guid='" + guid + '\'' +
                ", isEnabled=" + isEnabled +
                ", version=" + version +
                ", service='" + service + '\'' +
                ", name='" + name + '\'' +
                ", policyType=" + policyType +
                ", description='" + description + '\'' +
                ", isAuditEnabled=" + isAuditEnabled +
                ", isDenyAllElse=" + isDenyAllElse +
                ", policyPriority=" + policyPriority +
                ", policyLabels=" + policyLabels +
                ", resourceSignature='" + resourceSignature + '\'' +
                ", serviceType='" + serviceType + '\'' +
                '}';
    }
}
