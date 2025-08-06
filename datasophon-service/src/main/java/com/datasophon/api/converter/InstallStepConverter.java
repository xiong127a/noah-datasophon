package com.datasophon.api.converter;

import com.datasophon.common.converter.BaseConverter;
import com.datasophon.common.dto.InstallStepDTO;
import com.datasophon.common.vo.InstallStepVO;
import com.datasophon.dao.entity.InstallStepEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * 安装步骤转换器
 * 用于Entity、DTO、VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface InstallStepConverter extends BaseConverter<InstallStepEntity, InstallStepDTO, InstallStepVO> {

    InstallStepConverter INSTANCE = Mappers.getMapper(InstallStepConverter.class);

    @Override
    @Mapping(target = "displayStepName", source = "stepName")
    @Mapping(target = "displayStepDesc", source = "stepDesc")
    @Mapping(target = "installTypeValue", source = "installType")
    @Mapping(target = "installTypeText", source = "installType", qualifiedByName = "getInstallTypeDisplayText")
    @Mapping(target = "isValid", constant = "true")
    @Mapping(target = "stepOrder", ignore = true)
    InstallStepVO entityToVo(InstallStepEntity entity);

    @Override
    @Mapping(target = "displayStepName", expression = "java(dto.getDisplayStepName())")
    @Mapping(target = "displayStepDesc", expression = "java(dto.getDisplayStepDesc())")
    @Mapping(target = "installTypeValue", expression = "java(dto.getInstallTypeValue())")
    @Mapping(target = "installTypeText", expression = "java(getInstallTypeDisplayText(dto.getInstallTypeValue()))")
    @Mapping(target = "isValid", expression = "java(dto.isValid())")
    @Mapping(target = "stepOrder", ignore = true)
    InstallStepVO dtoToVo(InstallStepDTO dto);

    /**
     * 获取安装类型显示文本
     */
    @Named("getInstallTypeDisplayText")
    default String getInstallTypeDisplayText(Integer installTypeValue) {
        if (installTypeValue == null) {
            return "未知类型";
        }
        if (installTypeValue.equals(1)) {
            return "主机安装";
        } else if (installTypeValue.equals(2)) {
            return "服务安装";
        } else if (installTypeValue.equals(3)) {
            return "配置安装";
        } else {
            return "其他类型";
        }
    }

    /**
     * 批量转换DTO到VO（带顺序）
     */
    default InstallStepVO dtoToVoWithOrder(InstallStepDTO dto, Integer stepOrder) {
        InstallStepVO baseVo = dtoToVo(dto);
        return new InstallStepVO(
                baseVo.id(),
                baseVo.stepName(),
                baseVo.stepDesc(),
                baseVo.installType(),
                baseVo.displayStepName(),
                baseVo.displayStepDesc(),
                baseVo.installTypeValue(),
                baseVo.installTypeText(),
                baseVo.isValid(),
                stepOrder);
    }
}