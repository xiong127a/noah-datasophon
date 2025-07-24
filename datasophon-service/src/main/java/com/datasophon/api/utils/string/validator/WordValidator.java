package com.datasophon.api.utils.string.validator;

import cn.hutool.core.lang.Validator;

public class WordValidator implements StringValidator{

    private StringValidator nextValidator;

    @Override
    public void setNext(StringValidator nextValidator) {
        this.nextValidator = nextValidator;
    }

    @Override
    public void validate(String data) throws Exception {
        Validator.validateWord(data, "输入值只能为纯字母");
        if (nextValidator != null) {
            nextValidator.validate(data);
        }
    }
}
