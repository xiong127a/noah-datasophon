package com.datasophon.api.service;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.AutoScaleTaskVO;

public interface AutoScaleService {
    Result createAutoScaleTask(AutoScaleTaskVO taskVO);
    Result updateAutoScaleTask(AutoScaleTaskVO taskVO);
    Result getAutoScaleTasks(AutoScaleTaskVO taskVO);
    Result deleteAutoScaleTask(AutoScaleTaskVO taskVO);
}