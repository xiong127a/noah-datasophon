package com.datasophon.api.utils.string.validator;

import cn.hutool.core.lang.Validator;

public class GeneralValidator implements StringValidator {

    private StringValidator nextValidator;

    @Override
    public void setNext(StringValidator nextValidator) {
        this.nextValidator = nextValidator;
    }

    @Override
    public void validate(String data) throws Exception {
        Validator.validateGeneral(data, "输入值应该为 英文字母 、数字或下划线");
        if (nextValidator != null) {
            nextValidator.validate(data);
        }
    }
}
