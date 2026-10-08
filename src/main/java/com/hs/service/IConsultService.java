package com.hs.service;

import com.hs.entity.ConsultSession;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 咨询向导服务接口
 */
public interface IConsultService {

    /**
     * 创建咨询会话
     */
    ConsultSession createSession(String scenarioCode);

    /**
     * 提交回答并推进步骤
     */
    ConsultSession submitAnswer(Long sessionId, int step, String answer);

    /**
     * 完成咨询并生成报告
     */
    ConsultSession completeSession(Long sessionId);

    /**
     * 流式完成咨询会话
     */
    CompletableFuture<ConsultSession> completeSessionStreaming(Long sessionId,
                                                                Consumer<String> onPartialResponse);

    /**
     * 获取咨询会话
     */
    ConsultSession getSession(Long sessionId);
}
