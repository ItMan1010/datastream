package com.itman.datastream.security.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemLog {
    private Long systemLogId;
    private Integer type;
    private String username;
    private String ipAddress;
    private String moduleName;
    private String content;
    private String urlPath;
    private String userAgent;
    private String requestInfo;
    private String responseInfo;
}
