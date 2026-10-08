package com.hs.controller;

import com.hs.common.R;
import com.hs.entity.ChatMessage;
import com.hs.entity.ChatSession;
import com.hs.service.IChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 对话管理接口
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final IChatService chatService;

    public ChatController(IChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/session")
    public R<ChatSession> createSession(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        String scenario = params.get("scenario");
        return R.success(chatService.createSession(title, scenario));
    }

    @DeleteMapping("/session/{id}")
    public R<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return R.success();
    }

    @GetMapping("/sessions")
    public R<List<ChatSession>> listSessions() {
        return R.success(chatService.listSessions());
    }

    @GetMapping("/session/{id}")
    public R<ChatSession> getSession(@PathVariable Long id) {
        return R.success(chatService.getSession(id));
    }

    @PostMapping("/session/{sessionId}/message")
    public R<ChatMessage> sendMessage(@PathVariable Long sessionId, @RequestBody Map<String, String> params) {
        String content = params.get("content");
        if (content == null || content.isEmpty()) {
            return R.error(400, "消息内容不能为空");
        }
        return R.success(chatService.sendMessage(sessionId, content));
    }

    @PostMapping(value = "/session/{sessionId}/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStreaming(@PathVariable Long sessionId,
                                           @RequestBody Map<String, String> params) {
        String content = params.get("content");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        SseEmitter emitter = new SseEmitter(0L);
        chatService.sendMessageStreaming(sessionId, content, partialResponse -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("token")
                        .data(Map.of("content", partialResponse)));
            } catch (IOException exception) {
                throw new IllegalStateException("无法向客户端发送流式响应", exception);
            }
        }).whenComplete((message, error) -> {
            try {
                if (error != null) {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message", "AI 回复失败，请稍后重试")));
                } else {
                    emitter.send(SseEmitter.event().name("done").data(Map.of("complete", true)));
                }
                emitter.complete();
            } catch (IOException exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @GetMapping("/session/{sessionId}/messages")
    public R<List<ChatMessage>> getMessages(@PathVariable Long sessionId) {
        return R.success(chatService.getMessages(sessionId));
    }
}