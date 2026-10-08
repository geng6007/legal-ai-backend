package com.hs.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 通义千问 (DashScope) 配置<p>
 * 把大模型相关的对象注册为 Spring Bean，供业务层（Service）注入使用<p>
 * 整个后端 AI 能力的"模型接入点"，<p>
 *     统一封装了通义千问的对话、流式对话和向量化能力，<p>
 *         业务代码只需注入接口（ChatModel / StreamingChatModel / EmbeddingModel）即可，无需关心具体厂商实现。
 */
@Configuration
public class LLMConfig {

    @Value("${QIANWEN_API_KEY}")
    private String apiKey;

    /**
     *同步对话模型
     */
    @Bean
    public ChatModel chatModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen3.8-max")
                .temperature(0.7f)
                .build();
    }

    /**
     *流式对话模型
     */
    @Bean
    public StreamingChatModel streamingChatModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen3.8-max")
                .temperature(0.7f)
                .build();
    }

    /**
     *文本向量化模型<P>
     * 把文本转成向量，用于语义检索 / RAG（检索增强生成）
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v3")
                .build();
    }

    /**
     *向量存储
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}