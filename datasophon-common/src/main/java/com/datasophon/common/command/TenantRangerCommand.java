package com.datasophon.common.command;

import com.datasophon.common.enums.RangerOpType;
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

    // 操作类型 createService addUser deletePolicy
    RangerOpType operateType;

    Long clusterId;
    String serviceName;
    String roleName;
    List<String> userList;
    String tenantName;

}
