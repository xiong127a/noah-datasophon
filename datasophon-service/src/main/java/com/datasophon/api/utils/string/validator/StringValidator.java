package com.datasophon.api.utils.string.validator;

public interface StringValidator {
    void setNext(StringValidator nextValidator);
    void validate(String data) throws Exception;
}
