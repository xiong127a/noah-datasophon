package com.datasophon.api.converter;

import com.datasophon.common.dto.KubernetesResourceDTO;
import com.datasophon.common.vo.KubernetesResourceVO;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * Kubernetes资源VO转换器
 * DTO到VO的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper(componentModel = "spring")
@Component
public interface KubernetesResourceVOConverter {

    /**
     * DTO转VO
     */
    KubernetesResourceVO dtoToVo(KubernetesResourceDTO dto);

    /**
     * DTO列表转VO列表
     */
    java.util.List<KubernetesResourceVO> dtoListToVoList(java.util.List<KubernetesResourceDTO> dtoList);
}