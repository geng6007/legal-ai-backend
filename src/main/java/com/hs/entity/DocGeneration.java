package com.hs.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文书生成记录
 */
@Data
public class DocGeneration {
    private Long id;
    private String templateCode;
    private String title;
    private String content;
    private String filePath;
    private LocalDateTime createTime;
    private Integer deleted;
}