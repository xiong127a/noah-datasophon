package com.datasophon.common.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantRangerCommand implements Serializable {

    // 操作类型 createService addUser
    String operateType;

    Integer clusterId;
    String serviceName;
    String roleName;
    List<String> userList;

}
