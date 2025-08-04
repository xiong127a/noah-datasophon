/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.converter;

import com.datasophon.common.converter.BaseConverter;
import com.datasophon.common.dto.ClusterQueueCapacityDTO;
import com.datasophon.common.vo.ClusterQueueCapacityVO;
import com.datasophon.common.vo.ClusterQueueCapacityListVO;
import com.datasophon.dao.entity.ClusterQueueCapacity;
import com.datasophon.dao.model.ClusterQueueCapacityList;
import com.datasophon.dao.model.Links;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;

/**
 * 集群队列容量转换器
 * 负责ClusterQueueCapacity Entity、DTO、VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring")
public interface ClusterQueueCapacityConverter extends
        BaseConverter<ClusterQueueCapacity, ClusterQueueCapacityDTO, ClusterQueueCapacityVO> {

    /**
     * ClusterQueueCapacityList 转换为 ClusterQueueCapacityListVO
     */
    @Named("capacityListToListVO")
    default ClusterQueueCapacityListVO capacityListToListVO(ClusterQueueCapacityList capacityList) {
        if (capacityList == null) {
            return null;
        }

        List<ClusterQueueCapacityVO> nodes = entityListToVoList(capacityList.getNodes());
        List<ClusterQueueCapacityListVO.LinksVO> links = capacityList.getLinks().stream()
                .map(this::linksToLinksVO)
                .toList();

        return new ClusterQueueCapacityListVO(
                capacityList.getRootId(),
                nodes,
                links);
    }

    /**
     * Links 转换为 LinksVO
     */
    @Named("linksToLinksVO")
    default ClusterQueueCapacityListVO.LinksVO linksToLinksVO(Links links) {
        if (links == null) {
            return null;
        }
        return new ClusterQueueCapacityListVO.LinksVO(links.getFrom(), links.getTo());
    }
}