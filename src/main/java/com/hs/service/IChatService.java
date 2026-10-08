package com.hs.service;

import com.hs.entity.ChatMessage;
import com.hs.entity.ChatSession;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 对话服务接口
 */
public interface IChatService {

    /**
     * 创建新会话
     */
    ChatSession createSession(String title, String scenario);

    /**
     * 删除会话（逻辑删除）
     */
    void deleteSession(Long sessionId);

    /**
     * 获取会话列表
     */
    List<ChatSession> listSessions();

    /**
     * 获取会话详情
     */
    ChatSession getSession(Long sessionId);

    /**
     * 发送消息并获取 AI 回复
     */
    ChatMessage sendMessage(Long sessionId, String content);

    /**
     * 流式发送消息 - 异步流式<p>
     * 1. 返回值类型<p>
     * 返回 CompletableFuture<ChatMessage> - 表示异步操作的结果<p>
     * 可以在消息生成过程中获取部分响应，最终返回完整的 AI 回复消息<p>
     * 2. 参数说明<p>
     * sessionId: Long - 会话 ID，标识属于哪个对话会话<p>
     * content: String - 用户发送的消息内容<p>
     * onPartialResponse: Consumer<String> - 一个回调函数，用于处理 AI 回复的流式部分响应<p>
     * 3. 核心功能<p>
     * 这个方法是专门为实时流式对话设计的：<p>
     * 实时响应：AI 可以在生成回复的过程中，逐词逐句地实时返回部分结果，而不是等待整个回复生成完毕<p>
     * 用户体验：让用户能够看到 AI 正在思考并逐步生成回答的过程，提升交互体验<p>
     * 异步处理：使用 CompletableFuture 实现异步编程，避免阻塞主线程<p>
     * 4. 工作原理
     * 从实现类 ChatServiceImpl 中可以看到：<p>
     * 保存用户消息：首先将用户的消息保存到数据库<p>
     * 构建对话上下文：获取会话历史记录，构建完整的对话上下文<p>
     * 流式调用 AI：使用 streamingChatModel 调用支持流式响应的 AI 模型<p>
     * 实时回调：当 AI 返回部分响应时，通过 onPartialResponse 回调函数传递给前端<p>
     * 最终保存：当 AI 回复完成后，保存完整的 AI 消息到数据库并返回<p>
     * 5. 与普通方法的区别
     * 对比同接口中的 sendMessage 方法：<p>
     * sendMessage：同步阻塞，等待 AI 完整回复后才返回结果<p>
     * sendMessageStreaming：异步流式，立即返回 CompletableFuture，通过回调实时接收部分响应<p>
     * 应用场景<p>
     * 这种流式响应特别适合：<p>
     * 需要实时反馈的聊天应用<p>
     * 法律咨询等需要较长思考时间但希望给用户即时反馈的场景<p>
     * 移动端应用，提升用户体验<p>
     * 需要显示 AI "正在思考" 状态的场景<p>
     * 设计优势<p>
     * 响应迅速：用户不会等待整个回复生成完毕<p>
     * 资源友好：可以更早地开始处理和显示结果<p>
     * 用户体验好：看到 AI 逐步生成回答，体验更自然<p>
     * 错误处理：如果中途出错，可以及时中断并通知用户<p>
     * 这个方法是现代 AI 对话应用中的关键技术，为法律 AI 助手提供了流畅的实时对话体验。
     */
    CompletableFuture<ChatMessage> sendMessageStreaming(Long sessionId, String content,
                                                         Consumer<String> onPartialResponse);

    /**
     * 获取会话消息历史
     */
    List<ChatMessage> getMessages(Long sessionId);
}
