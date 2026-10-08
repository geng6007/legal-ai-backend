package com.hs.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 咨询向导会话
 */
@Data
public class ConsultSession {
    private Long id;
    private String scenarioCode;
    private String status;
    private Integer currentStep;
    private String answers;
    private String report;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}