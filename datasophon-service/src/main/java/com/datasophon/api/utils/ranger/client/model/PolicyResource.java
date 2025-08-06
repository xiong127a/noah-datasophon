package com.datasophon.api.utils.ranger.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyResource {
    @Builder.Default
    private List<String> values = Lists.newArrayList();
    private Boolean isExcludes;
    private Boolean isRecursive;

    @Override
    public String toString() {
        return "PolicyResource{" +
                "values=" + values +
                ", isExcludes=" + isExcludes +
                ", isRecursive=" + isRecursive +
                '}';
    }
}
