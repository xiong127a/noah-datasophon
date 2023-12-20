package com.datasophon.api.utils.ranger.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role {
    private Integer id;
    private String guid;
    private Boolean isEnabled;
    private Integer version;
    private List<RoleMember> groups;
    private List<RoleMember> users;
    private List<RoleMember> roles;
    private Map<Object, Object> options;
    private String description;
    private String name;

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", guid='" + guid + '\'' +
                ", isEnabled=" + isEnabled +
                ", version=" + version +
                ", groups=" + groups +
                ", users=" + users +
                ", roles=" + roles +
                ", options=" + options +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
