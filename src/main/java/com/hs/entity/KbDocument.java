package com.hs.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档
 */
@Data
public class KbDocument {
    private Long id;
    private String title;
    private String source;
    private String docType;
    private Integer chunkCount;
    private String content;
    private LocalDateTime createTime;
    private Integer deleted;
}