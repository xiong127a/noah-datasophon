package com.datasophon.api.utils.StringValidator;

public interface StringValidator {
    void setNext(StringValidator nextValidator);
    void validate(String data) throws Exception;
}
