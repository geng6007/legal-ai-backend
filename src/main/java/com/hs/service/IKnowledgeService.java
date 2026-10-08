package com.hs.service;

import com.hs.entity.KbDocument;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface IKnowledgeService {

    /**
     * 添加文档并自动生成向量索引
     */
    KbDocument addDocument(String title, String source, String docType, String content);

    /**
     * 添加 PDF 文档
     */
    KbDocument addPdfDocument(String title, String source, String docType, byte[] pdfBytes);

    /**
     * 删除文档
     */
    void deleteDocument(Long id);

    /**
     * 获取文档详情
     */
    KbDocument getDocument(Long id);

    /**
     * 获取文档列表
     */
    List<KbDocument> listDocuments(String docType);

    /**
     * 向量检索相关文档
     */
    List<String> searchRelevant(String query, int maxResults);
}
