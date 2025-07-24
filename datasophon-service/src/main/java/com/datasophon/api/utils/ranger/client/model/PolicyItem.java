package com.datasophon.api.utils.ranger.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyItem {
    @Builder.Default
    private List<PolicyItemAccess> accesses = Lists.newArrayList();
    @Builder.Default
    private Set<String> users = Sets.newHashSet();
    @Builder.Default
    private List<String> groups = Lists.newArrayList();
    @Builder.Default
    private List<String> roles = Lists.newArrayList();
    @Builder.Default
    private List<PolicyItemCondition> conditions = Lists.newArrayList();
    private Boolean delegateAdmin;

    @Override
    public String toString() {
        return "PolicyItem{" +
                "accesses=" + accesses +
                ", users=" + users +
                ", groups=" + groups +
                ", conditions=" + conditions +
                ", delegateAdmin=" + delegateAdmin +
                '}';
    }
}
