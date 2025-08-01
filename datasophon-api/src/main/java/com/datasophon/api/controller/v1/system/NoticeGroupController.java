/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.controller.v1.system;

import com.datasophon.api.service.NoticeGroupService;
import com.datasophon.api.converter.NoticeGroupConverter;
import com.datasophon.api.vo.Result;
import com.datasophon.api.vo.NoticeGroupVO;
import com.datasophon.common.dto.NoticeGroupDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 通知组控制器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@ApiVersion(path = "notice/group")
public class NoticeGroupController {

    private final NoticeGroupService noticeGroupService;
    private final NoticeGroupConverter noticeGroupConverter;

    public NoticeGroupController(NoticeGroupService noticeGroupService, NoticeGroupConverter noticeGroupConverter) {
        this.noticeGroupService = noticeGroupService;
        this.noticeGroupConverter = noticeGroupConverter;
    }

    /**
     * 获取通知组分页列表
     */
    @GetMapping("/list")
    public Result<PageResult<NoticeGroupVO>> list(
            @RequestParam(value = "noticeGroupName", required = false) String noticeGroupName,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        PageResult<NoticeGroupDTO> dtoPageResult = noticeGroupService.getNoticeGroupList(
                noticeGroupName, page, pageSize);

        List<NoticeGroupVO> voList = noticeGroupConverter.dtoListToVoList(dtoPageResult.getRecords());
        PageResult<NoticeGroupVO> voPageResult = PageResult.of(voList, dtoPageResult.getTotal(), page, pageSize);

        return Result.success(voPageResult);
    }

    /**
     * 获取通知组详情
     */
    @GetMapping("/info/{id}")
    public Result<NoticeGroupVO> info(@PathVariable("id") Integer id) {
        NoticeGroupDTO noticeGroupDTO = noticeGroupService.getNoticeGroupById(id);

        if (noticeGroupDTO == null) {
            return Result.error("通知组不存在");
        }

        NoticeGroupVO noticeGroupVO = noticeGroupConverter.dtoToVo(noticeGroupDTO);
        return Result.success(noticeGroupVO);
    }

    /**
     * 创建通知组
     */
    @PostMapping("/save")
    public Result<NoticeGroupVO> save(@RequestBody NoticeGroupDTO noticeGroupDTO) {
        try {
            NoticeGroupDTO savedDTO = noticeGroupService.saveNoticeGroup(noticeGroupDTO);
            NoticeGroupVO noticeGroupVO = noticeGroupConverter.dtoToVo(savedDTO);
            return Result.success(noticeGroupVO);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新通知组
     */
    @PutMapping("/update")
    public Result<NoticeGroupVO> update(@RequestBody NoticeGroupDTO noticeGroupDTO) {
        try {
            NoticeGroupDTO updatedDTO = noticeGroupService.updateNoticeGroup(noticeGroupDTO);
            NoticeGroupVO noticeGroupVO = noticeGroupConverter.dtoToVo(updatedDTO);
            return Result.success(noticeGroupVO);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除通知组
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Integer[] ids) {
        try {
            List<Integer> idList = Arrays.asList(ids);
            boolean deleted = noticeGroupService.deleteNoticeGroups(idList);

            return deleted ? Result.success("删除成功") : Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有通知组
     */
    @GetMapping("/all")
    public Result<List<NoticeGroupVO>> getAllNoticeGroups() {
        List<NoticeGroupDTO> dtoList = noticeGroupService.getAllNoticeGroups();
        List<NoticeGroupVO> voList = noticeGroupConverter.dtoListToVoList(dtoList);
        return Result.success(voList);
    }

}
