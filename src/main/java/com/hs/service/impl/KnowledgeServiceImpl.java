package com.hs.service.impl;

import com.hs.config.SnowflakeIdGenerator;
import com.hs.entity.KbDocument;
import com.hs.mapper.KbDocumentMapper;
import com.hs.service.IKnowledgeService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.io.IOException;
import java.util.List;

/**
 * 知识库服务实现
 */
@Service
public class KnowledgeServiceImpl implements IKnowledgeService {

    private final KbDocumentMapper documentMapper;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final SnowflakeIdGenerator idGenerator;

    public KnowledgeServiceImpl(KbDocumentMapper documentMapper,
                                EmbeddingModel embeddingModel,
                                EmbeddingStore<TextSegment> embeddingStore,
                                SnowflakeIdGenerator idGenerator) {
        this.documentMapper = documentMapper;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.idGenerator = idGenerator;
    }

    /**
     * 添加文档并自动生成向量索引
     */
    @Override
    @Transactional
    public KbDocument addDocument(String title, String source, String docType, String content) {
        KbDocument doc = new KbDocument();
        doc.setId(idGenerator.nextId());
        doc.setTitle(title);
        doc.setSource(source);
        doc.setDocType(docType);
        doc.setContent(content);
        doc.setCreateTime(LocalDateTime.now());
        doc.setDeleted(0);

        // 切分文档并生成嵌入向量
        if (content != null && !content.isEmpty()) {
            List<String> chunks = splitContent(content);
            doc.setChunkCount(chunks.size());

            for (String chunk : chunks) {
                Embedding embedding = embeddingModel.embed(chunk).content();
                embeddingStore.add(embedding, TextSegment.from(chunk));
            }
        }

        documentMapper.insert(doc);
        return doc;
    }

    @Override
    @Transactional
    public KbDocument addPdfDocument(String title, String source, String docType, byte[] pdfBytes) {
        String content;
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            content = new PDFTextStripper().getText(pdf).trim();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("PDF 文件无法读取，请确认文件未损坏且未加密", exception);
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("PDF 中没有可提取的文本，扫描版 PDF 暂不支持");
        }
        return addDocument(title, source, docType, content);
    }

    /**
     * 删除文档
     */
    @Override
    @Transactional
    public void deleteDocument(Long id) {
        documentMapper.logicDeleteById(id);
    }

    /**
     * 获取文档详情
     */
    @Override
    public KbDocument getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    /**
     * 获取文档列表
     */
    @Override
    public List<KbDocument> listDocuments(String docType) {
        if (docType != null && !docType.isEmpty()) {
            return documentMapper.selectByDocType(docType);
        }
        return documentMapper.selectAll();
    }

    /**
     * 向量检索相关文档
     */
    @Override
    public List<String> searchRelevant(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        return result.matches().stream()
                .map(match -> match.embedded().text())
                .toList();
    }

    /**
     * 简单文本切分
     */
    private List<String> splitContent(String content) {
        int chunkSize = 500;
        int overlap = 50;
        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            chunks.add(content.substring(start, end));
            start += chunkSize - overlap;
        }
        return chunks;
    }
}
