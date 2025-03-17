package com.datasophon.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckItem implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public enum Status {
        WAITING(0, "待检查"),
        SUCCESS(1, "通过"),
        FAILED(2, "未通过"),
        CHECKING(3, "检查中"),
        SKIPPED(4, "已跳过"),
        TERMINATING(5, "终止中");
        
        private final int code;
        private final String desc;
        
        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDesc() {
            return desc;
        }
        
        public static Status getByCode(int code) {
            for (Status status : Status.values()) {
                if (status.getCode() == code) {
                    return status;
                }
            }
            return WAITING;
        }
    }

    /**
     * 检查项ID
     */
    private Integer id;

    private String itemCode;

    /**
     * 检查项名称
     */
    private String itemName;
    
    /**
     * 检查状态
     */
    @Builder.Default
    private Status status = Status.WAITING;
    
    /**
     * 检查结果信息
     */
    private String message;
} 