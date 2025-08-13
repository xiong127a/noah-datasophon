package com.datasophon.api.controller.v1.system;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.converter.AutoScaleTaskConverter;
import com.datasophon.api.service.AutoScaleService;
import com.datasophon.common.dto.AutoScaleTaskDTO;
import com.datasophon.common.enums.Status;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.vo.AutoScaleTaskVO;
import com.datasophon.api.dto.Result;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自动伸缩控制器
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "autoScale")
public class AutoScaleController {

    private static final Logger logger = LoggerFactory.getLogger(AutoScaleController.class);

    @Autowired
    private AutoScaleService autoScaleService;

    @Autowired
    private AutoScaleTaskConverter autoScaleTaskConverter;

    /**
     * 创建自动伸缩任务
     */
    @PostMapping("/createAutoScaleTask")
    public Result<AutoScaleTaskVO> createAutoScaleTask(@RequestBody @Valid AutoScaleTaskDTO taskDTO) {
        try {
            logger.info("创建自动伸缩任务，任务名称: {}, 集群ID: {}", taskDTO.taskName(), taskDTO.clusterId());

            AutoScaleTaskDTO createdTask = autoScaleService.createAutoScaleTask(taskDTO);
            AutoScaleTaskVO taskVO = autoScaleTaskConverter.dtoToVo(createdTask);

            return Result.success(taskVO)
                    .setMsg("创建自动伸缩任务成功")
                    .setCode(Status.SUCCESS.getCode());

        } catch (Exception e) {
            logger.error("创建自动伸缩任务失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新自动伸缩任务
     */
    @PostMapping("/updateAutoScaleTask")
    public Result<AutoScaleTaskVO> updateAutoScaleTask(@RequestBody @Valid AutoScaleTaskDTO taskDTO) {
        try {
            logger.info("更新自动伸缩任务，任务ID: {}", taskDTO.id());

            AutoScaleTaskDTO updatedTask = autoScaleService.updateAutoScaleTask(taskDTO);
            AutoScaleTaskVO taskVO = autoScaleTaskConverter.dtoToVo(updatedTask);

            return Result.success(taskVO)
                    .setMsg("更新自动伸缩任务成功")
                    .setCode(Status.SUCCESS.getCode());

        } catch (Exception e) {
            logger.error("更新自动伸缩任务失败: {}", e.getMessage(), e);
            return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), e.getMessage());
        }
    }

    /**
     * 分页查询自动伸缩任务
     */
    @GetMapping("/getAutoScaleTasks")
    public Result<PageResult<AutoScaleTaskVO>> getAutoScaleTasks(
            @RequestParam Long clusterId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            logger.info("查询自动伸缩任务，集群ID: {}, 页码: {}, 页大小: {}", clusterId, page, pageSize);

            PageResult<AutoScaleTaskDTO> pageResult = autoScaleService.getAutoScaleTasks(clusterId, page, pageSize);
            List<AutoScaleTaskVO> voList = autoScaleTaskConverter.dtoListToVoList(pageResult.getRecords());
            PageResult<AutoScaleTaskVO> voPageResult = PageResult.of(voList, pageResult.getTotal(), page, pageSize);

            return Result.success(voPageResult)
                    .setMsg("查询自动伸缩任务成功")
                    .setCode(Status.SUCCESS.getCode());

        } catch (Exception e) {
            logger.error("查询自动伸缩任务失败: {}", e.getMessage(), e);
            return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), e.getMessage());
        }
    }

    /**
     * 获取集群启用的自动伸缩任务
     */
    @GetMapping("/getEnabledTasks")
    public Result<List<AutoScaleTaskVO>> getEnabledTasks(@RequestParam Long clusterId) {
        try {
            logger.info("查询集群启用的自动伸缩任务，集群ID: {}", clusterId);

            List<AutoScaleTaskDTO> taskList = autoScaleService.getEnabledTasksByClusterId(clusterId);
            List<AutoScaleTaskVO> voList = autoScaleTaskConverter.dtoListToVoList(taskList);

            return Result.success(voList)
                    .setMsg("查询启用任务成功")
                    .setCode(Status.SUCCESS.getCode());

        } catch (Exception e) {
            logger.error("查询启用任务失败: {}", e.getMessage(), e);
            return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), e.getMessage());
        }
    }

    /**
     * 删除自动伸缩任务
     */
    @DeleteMapping("/deleteAutoScaleTask/{taskId}")
    public Result<Void> deleteAutoScaleTask(@PathVariable Long taskId) {
        try {
            logger.info("删除自动伸缩任务，任务ID: {}", taskId);

            boolean success = autoScaleService.deleteAutoScaleTask(taskId);
            if (success) {
                return Result.<Void>success()
                        .setMsg("删除自动伸缩任务成功")
                        .setCode(Status.SUCCESS.getCode());
            } else {
                return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), "删除失败");
            }

        } catch (Exception e) {
            logger.error("删除自动伸缩任务失败: {}", e.getMessage(), e);
            return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), e.getMessage());
        }
    }

    /**
     * 启用/禁用自动伸缩任务
     */
    @PostMapping("/toggleTask")
    public Result<AutoScaleTaskVO> toggleAutoScaleTask(
            @RequestParam Long taskId,
            @RequestParam Boolean enabled) {
        try {
            logger.info("切换自动伸缩任务状态，任务ID: {}, 状态: {}", taskId, enabled);

            AutoScaleTaskDTO updatedTask = autoScaleService.toggleAutoScaleTask(taskId, enabled);
            AutoScaleTaskVO taskVO = autoScaleTaskConverter.dtoToVo(updatedTask);

            return Result.success(taskVO)
                    .setMsg(enabled ? "启用任务成功" : "禁用任务成功")
                    .setCode(Status.SUCCESS.getCode());

        } catch (Exception e) {
            logger.error("切换任务状态失败: {}", e.getMessage(), e);
            return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), e.getMessage());
        }
    }

    /**
     * 检查集群自动伸缩状态
     */
    @GetMapping("/checkAutoScaleStatus")
    public Result<Boolean> checkAutoScaleStatus(@RequestParam Long clusterId) {
        try {
            logger.info("检查集群自动伸缩状态，集群ID: {}", clusterId);

            boolean enabled = autoScaleService.isAutoScaleEnabled(clusterId);

            return Result.success(enabled)
                    .setMsg("查询状态成功")
                    .setCode(Status.SUCCESS.getCode());

        } catch (Exception e) {
            logger.error("查询自动伸缩状态失败: {}", e.getMessage(), e);
            return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), e.getMessage());
        }
    }
}
