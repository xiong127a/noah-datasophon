
package com.datasophon.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.enums.Status;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.OperationLogService;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.EncryptionUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.OperationLog;
import com.datasophon.dao.entity.OperationLog;
import com.datasophon.dao.model.MPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("api/log")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 列表带分页
     */
    @RequestMapping("/list")
    public Result list(MPage<OperationLog> mPage) {
        return Result.success(operationLogService.pageOperationLog(mPage));
    }


}
