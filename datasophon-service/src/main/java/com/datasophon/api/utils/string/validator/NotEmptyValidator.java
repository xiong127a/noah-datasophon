package com.datasophon.api.utils.string.validator;

import cn.hutool.core.lang.Validator;

public class NotEmptyValidator implements StringValidator {

    private StringValidator nextValidator;

    @Override
    public void setNext(StringValidator nextValidator) {
        this.nextValidator = nextValidator;
    }

    @Override
    public void validate(String data) throws Exception {
        Validator.validateNotEmpty(data, "输入值不能为空");
        if (nextValidator != null) {
            nextValidator.validate(data);
        }
    }
}
