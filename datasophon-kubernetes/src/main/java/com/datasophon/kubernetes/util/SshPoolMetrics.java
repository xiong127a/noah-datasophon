package com.datasophon.kubernetes.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SshPoolMetrics {
    private Integer numActive;
    private Integer numIdle;
    private Integer numWaiters;
}