package com.hs.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同审查任务
 */
@Data
public class ContractReviewTask {
    private Long id;
    private String fileName;
    private Integer clauseCount;
    private Integer riskCount;
    private String status;
    private String result;
    private LocalDateTime createTime;
    private Integer deleted;
}