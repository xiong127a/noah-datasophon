package com.datasophon.api.controller;

import com.datasophon.api.service.AutoScaleService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.AutoScaleTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 63588
 */
@RestController
@RequestMapping("api/autoScale")
public class AutoScaleController {

    @Autowired
    private AutoScaleService autoScaleService;


    @PostMapping("/createAutoScaleTask")
    public Result createAutoScaleTask(@RequestBody AutoScaleTaskVO taskVO) {
        return autoScaleService.createAutoScaleTask(taskVO);
    }

    @PostMapping("/updateAutoScaleTask")
    public Result updateAutoScaleTask(@RequestBody AutoScaleTaskVO taskVO) {
        return autoScaleService.updateAutoScaleTask(taskVO);
    }

    @PostMapping("/getAutoScaleTasks")
    public Result getAutoScaleTasks(@RequestBody AutoScaleTaskVO taskVO) {
        return autoScaleService.getAutoScaleTasks(taskVO);
    }

    @PostMapping("/deleteAutoScaleTask")
    public Result deleteAutoScaleTask(@RequestBody AutoScaleTaskVO taskVO) {
        return autoScaleService.deleteAutoScaleTask(taskVO);
    }
}

