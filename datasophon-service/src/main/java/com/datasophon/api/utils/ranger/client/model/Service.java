package com.datasophon.api.utils.ranger.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Service {
    private Long id;
    private String guid;
    private Boolean isEnabled;
    private String createdBy;
    private String updatedBy;
    private Long createTime;
    private Long updateTime;
    private Long version;
    private String type;
    private String name;
    private String description;
    private Map<String, String> configs;
    private Integer policyVersion;
    private Long policyUpdateTime;
    private Integer tagVersion;
    private Long tagUpdateTime;

    @Override
    public String toString() {
        return "Service{" +
                "id=" + id +
                ", guid='" + guid + '\'' +
                ", isEnabled=" + isEnabled +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", version=" + version +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", config=" + configs +
                ", policyVersion=" + policyVersion +
                ", policyUpdateTime=" + policyUpdateTime +
                ", tagVersion=" + tagVersion +
                ", tagUpdateTime=" + tagUpdateTime +
                '}';
    }
}
