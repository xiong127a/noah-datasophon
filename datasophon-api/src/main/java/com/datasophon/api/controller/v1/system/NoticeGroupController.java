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

package com.datasophon.api.controller;

import com.datasophon.api.service.impl.NoticeGroupServiceImpl;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.NoticeGroupEntity;
import com.datasophon.dao.model.MPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;


/**
 * 通知组
 */
@RestController
@RequestMapping("api/notice/group")
public class NoticeGroupController {

    @Autowired
    private NoticeGroupServiceImpl noticeGroupService;


    /**
     * 列表带分页
     */
    @RequestMapping("/list")
    public Result list(MPage<NoticeGroupEntity> mPage) {
        return Result.success(noticeGroupService.pageNoticeGroup(mPage));
    }


    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody NoticeGroupEntity noticeGroup) {
        List<String> existGroup = noticeGroupService.list()
                .stream()
                .map(NoticeGroupEntity::getNoticeGroupName)
                .toList();
        if (existGroup.contains(noticeGroup.getNoticeGroupName())) {
            return Result.error("通知组名称重复");
        }
        return noticeGroupService.saveOrUpdateNoticeGroup(noticeGroup);
//        return Result.success();
    }


    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody NoticeGroupEntity noticeGroup) {
        return noticeGroupService.saveOrUpdateNoticeGroup(noticeGroup);
//        return Result.success();
    }


    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids) {
        noticeGroupService.removeNoticeGroup(Arrays.asList(ids));
        return Result.success();
    }

}
