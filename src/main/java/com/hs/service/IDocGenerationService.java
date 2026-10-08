package com.hs.service;

import com.hs.entity.DocGeneration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 文书生成服务接口
 */
public interface IDocGenerationService {

    /**
     * 生成法律文书
     */
    DocGeneration generateDocument(String templateCode, String title, String params);

    /**
     * 流式生成法律文书
     */
    CompletableFuture<DocGeneration> generateDocumentStreaming(
            String templateCode, String title, String params, Consumer<String> onPartialResponse);

    /**
     * 获取文书记录
     */
    DocGeneration getDocument(Long id);

    /**
     * 获取文书列表
     */
    List<DocGeneration> listDocuments();
}
