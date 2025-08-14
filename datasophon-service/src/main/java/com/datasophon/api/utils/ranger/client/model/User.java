package com.datasophon.api.utils.ranger.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private Long id;
    private String createDate;
    private String updateDate;
    private String owner;
    private String updatedBy;
    private String name;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private String password;
    private String description;
    private Integer status;
    private Integer isVisible;
    private Integer userSource;
    private List<String> userRoleList;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", createDate='" + createDate + '\'' +
                ", updateDate='" + updateDate + '\'' +
                ", owner='" + owner + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", name='" + name + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", password='" + password + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", isVisible=" + isVisible +
                ", userSource=" + userSource +
                ", userRoleList=" + userRoleList +
                '}';
    }
}
