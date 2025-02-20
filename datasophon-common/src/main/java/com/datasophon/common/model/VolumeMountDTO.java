package com.datasophon.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VolumeMountDTO {
    String volumeName;
    String hostPath;
    String containerPath;
}