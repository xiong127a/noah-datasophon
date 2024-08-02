package com.datasophon.api.utils.StringValidator;

import cn.hutool.core.util.StrUtil;

public class LengthValidator implements StringValidator {

    private StringValidator nextValidator;

    @Override
    public void setNext(StringValidator nextValidator) {
        this.nextValidator = nextValidator;
    }

    @Override
    public void validate(String data) throws Exception {
        if (StrUtil.isBlank(data)) {
            throw new IllegalArgumentException("输入值不能为空");
        }
        if (data.length() > 255) {
            throw new IllegalArgumentException("输入值长度不能超过255");
        }
        if (nextValidator != null) {
            nextValidator.validate(data);
        }
    }
}
