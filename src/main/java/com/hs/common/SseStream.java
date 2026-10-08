package com.hs.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SSE（Server-Sent Events）流式响应工具类，主要用于向客户端推送实时流式数据<p>
 * 核心功能：<p>
 * 创建 SSE 连接 (createEmitter):<p>
 *     创建一个永不超时的 SseEmitter<p>
 *     监听客户端断开连接的错误事件<p>
 * 发送流式事件 (send):<p>
 *     封装了底层的 SSE 事件发送逻辑<p>
 *     以 JSON 格式发送数据，支持不同类型的事件名<p>
 * 标准化的事件流程:<p>
 *     sendStart - 发送开始事件，标记流式响应开始<p>
 *     sendToken - 发送 AI 生成的文本片段（token），实现打字机效果<p>
 *     finish - 处理异步任务完成，发送最终结果或错误信息<p>
 */
public final class SseStream {

    private static final Logger log = LoggerFactory.getLogger(SseStream.class);

    private SseStream() {
    }

    public static SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onError(error -> log.warn("SSE client disconnected", error));
        return emitter;
    }

    public static void sendStart(SseEmitter emitter, Object data) {
        send(emitter, "start", data);
    }

    public static void sendToken(SseEmitter emitter, String content) {
        send(emitter, "token", Map.of("content", content));
    }

    public static <T> void finish(SseEmitter emitter, CompletableFuture<T> result) {
        result.whenComplete((data, error) -> {
            if (error != null) {
                log.error("Streaming AI generation failed", error);
                try {
                    send(emitter, "error", Map.of("message", "AI 生成失败，请稍后重试"));
                    emitter.complete();
                } catch (RuntimeException sendError) {
                    emitter.completeWithError(sendError);
                }
                return;
            }

            try {
                send(emitter, "done", data);
                emitter.complete();
            } catch (RuntimeException sendError) {
                emitter.completeWithError(sendError);
            }
        });
    }

    private static void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            throw new IllegalStateException("无法向客户端发送流式响应", exception);
        }
    }
}
