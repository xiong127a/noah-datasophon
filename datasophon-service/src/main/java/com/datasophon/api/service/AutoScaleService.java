package com.datasophon.api.service;

import com.datasophon.api.vo.Result;
import com.datasophon.dao.entity.AutoScaleTaskVO;

public interface AutoScaleService {
    Result createAutoScaleTask(AutoScaleTaskVO taskVO);
    Result updateAutoScaleTask(AutoScaleTaskVO taskVO);
    Result getAutoScaleTasks(AutoScaleTaskVO taskVO);
    Result deleteAutoScaleTask(AutoScaleTaskVO taskVO);
}