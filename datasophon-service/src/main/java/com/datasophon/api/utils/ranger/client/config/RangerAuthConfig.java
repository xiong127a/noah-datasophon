package com.datasophon.api.utils.ranger.client.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RangerAuthConfig {
    @Builder.Default
    private String username = "admin";
    @Builder.Default
    private String password = "admin";

    @Override
    public String toString() {
        return "RangerAuthConfig{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}