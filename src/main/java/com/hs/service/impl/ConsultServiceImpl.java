package com.hs.service.impl;

import com.hs.config.SnowflakeIdGenerator;
import com.hs.entity.ConsultSession;
import com.hs.mapper.ConsultSessionMapper;
import com.hs.service.IConsultService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 咨询向导服务实现
 */
@Service
public class ConsultServiceImpl implements IConsultService {

    private final ConsultSessionMapper sessionMapper;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final SnowflakeIdGenerator idGenerator;

    public ConsultServiceImpl(ConsultSessionMapper sessionMapper,
                              ChatModel chatModel,
                              StreamingChatModel streamingChatModel,
                              SnowflakeIdGenerator idGenerator) {
        this.sessionMapper = sessionMapper;
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.idGenerator = idGenerator;
    }

    /**
     * 创建咨询会话
     */
    @Override
    @Transactional
    public ConsultSession createSession(String scenarioCode) {
        ConsultSession session = new ConsultSession();
        session.setId(idGenerator.nextId());
        session.setScenarioCode(scenarioCode);
        session.setStatus("IN_PROGRESS");
        session.setCurrentStep(1);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setDeleted(0);
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 提交回答并推进步骤
     */
    @Override
    @Transactional
    public ConsultSession submitAnswer(Long sessionId, int step, String answer) {
        ConsultSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("咨询会话不存在");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalArgumentException("咨询会话已结束");
        }

        // 保存回答
        java.util.Map<String, Object> answersMap;
        if (session.getAnswers() != null && !session.getAnswers().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                answersMap = mapper.readValue(session.getAnswers(), java.util.Map.class);
            } catch (Exception e) {
                answersMap = new java.util.HashMap<>();
            }
        } else {
            answersMap = new java.util.HashMap<>();
        }
        answersMap.put("step_" + step, answer);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            session.setAnswers(mapper.writeValueAsString(answersMap));
        } catch (Exception e) {
            session.setAnswers("{\"step_" + step + "\":\"" + answer + "\"}");
        }

        session.setCurrentStep(step + 1);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.update(session);
        return session;
    }

    /**
     * 完成咨询并生成报告
     */
    @Override
    @Transactional
    public ConsultSession completeSession(Long sessionId) {
        ConsultSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("咨询会话不存在");
        }

        // 生成报告
        String prompt = "你是法律咨询助手。请根据以下用户提供的信息，生成一份专业的法律咨询报告。" +
                "场景编码：" + session.getScenarioCode() + "\n" +
                "用户回答：" + session.getAnswers() + "\n" +
                "请给出法律分析、风险评估和建议。";

        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .build();
        ChatResponse response = chatModel.chat(request);
        String report = response.aiMessage().text();

        session.setReport(report);
        session.setStatus("COMPLETED");
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.update(session);
        return session;
    }

    @Override
    public CompletableFuture<ConsultSession> completeSessionStreaming(Long sessionId,
                                                                      Consumer<String> onPartialResponse) {
        ConsultSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("咨询会话不存在");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalArgumentException("咨询会话已结束");
        }

        String prompt = "你是法律咨询助手。请根据以下用户提供的信息，生成一份专业的法律咨询报告。" +
                "场景编码：" + session.getScenarioCode() + "\n" +
                "用户回答：" + session.getAnswers() + "\n" +
                "请给出法律分析、风险评估和建议。";
        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .build();
        StringBuilder reportBuilder = new StringBuilder();
        CompletableFuture<ConsultSession> result = new CompletableFuture<>();

        try {
            streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    reportBuilder.append(partialResponse);
                    onPartialResponse.accept(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    try {
                        session.setReport(reportBuilder.toString());
                        session.setStatus("COMPLETED");
                        session.setUpdateTime(LocalDateTime.now());
                        sessionMapper.update(session);
                        result.complete(session);
                    } catch (RuntimeException exception) {
                        result.completeExceptionally(exception);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }
            });
        } catch (RuntimeException exception) {
            result.completeExceptionally(exception);
        }
        return result;
    }

    /**
     * 获取咨询会话
     */
    @Override
    public ConsultSession getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }
}
