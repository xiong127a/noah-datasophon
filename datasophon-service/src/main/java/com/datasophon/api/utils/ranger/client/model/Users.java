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
public class Users {
    private Integer startIndex;
    private Integer pageSize;
    private Integer totalCount;
    private Integer resultSize;
    private String sortType;
    private String sortBy;
    private Integer queryTimeMS;
    private List<User> vXUsers;

    @Override
    public String toString() {
        return "Users{" +
                "startIndex=" + startIndex +
                ", pageSize=" + pageSize +
                ", totalCount=" + totalCount +
                ", resultSize=" + resultSize +
                ", sortType='" + sortType + '\'' +
                ", sortBy='" + sortBy + '\'' +
                ", queryTimeMS=" + queryTimeMS +
                ", vXUsers=" + vXUsers +
                '}';
    }
}
