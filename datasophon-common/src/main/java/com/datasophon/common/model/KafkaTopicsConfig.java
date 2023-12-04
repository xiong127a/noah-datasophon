package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class KafkaTopicsConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String topic;
    private String capacity;
    private String replicas;

}
