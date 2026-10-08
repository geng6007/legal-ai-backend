package com.hs.controller;

import com.hs.common.R;
import com.hs.common.SseStream;
import com.hs.entity.ConsultSession;
import com.hs.service.IConsultService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 咨询向导接口
 */
@RestController
@RequestMapping("/api/consult")
public class ConsultController {

    private final IConsultService consultService;

    public ConsultController(IConsultService consultService) {
        this.consultService = consultService;
    }

    @PostMapping("/session")
    public R<ConsultSession> createSession(@RequestBody Map<String, String> params) {
        String scenarioCode = params.get("scenarioCode");
        if (scenarioCode == null || scenarioCode.isEmpty()) {
            return R.error(400, "场景编码不能为空");
        }
        return R.success(consultService.createSession(scenarioCode));
    }

    @PostMapping("/session/{sessionId}/answer")
    public R<ConsultSession> submitAnswer(@PathVariable Long sessionId, @RequestBody Map<String, Object> params) {
        int step = params.get("step") instanceof Integer ? (Integer) params.get("step") : Integer.parseInt(params.get("step").toString());
        String answer = (String) params.get("answer");
        return R.success(consultService.submitAnswer(sessionId, step, answer));
    }

    @PostMapping("/session/{sessionId}/complete")
    public R<ConsultSession> completeSession(@PathVariable Long sessionId) {
        return R.success(consultService.completeSession(sessionId));
    }

    @PostMapping(value = "/session/{sessionId}/complete/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter completeSessionStreaming(@PathVariable Long sessionId) {
        SseEmitter emitter = SseStream.createEmitter();
        SseStream.finish(emitter, consultService.completeSessionStreaming(
                sessionId, token -> SseStream.sendToken(emitter, token)));
        return emitter;
    }

    @GetMapping("/session/{id}")
    public R<ConsultSession> getSession(@PathVariable Long id) {
        return R.success(consultService.getSession(id));
    }
}