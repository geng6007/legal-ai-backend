package com.hs.controller;

import com.hs.common.R;
import com.hs.common.SseStream;
import com.hs.entity.DocGeneration;
import com.hs.service.IDocGenerationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 文书生成接口
 */
@RestController
@RequestMapping("/api/doc")
public class DocGenerationController {

    private final IDocGenerationService docGenerationService;

    public DocGenerationController(IDocGenerationService docGenerationService) {
        this.docGenerationService = docGenerationService;
    }

    @PostMapping("/generate")
    public R<DocGeneration> generate(@RequestBody Map<String, String> params) {
        String templateCode = params.get("templateCode");
        String title = params.get("title");
        String paramsJson = params.get("params");
        if (templateCode == null || title == null) {
            return R.error(400, "模板编码和标题不能为空");
        }
        return R.success(docGenerationService.generateDocument(templateCode, title, paramsJson));
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStreaming(@RequestBody Map<String, String> params) {
        String templateCode = params.get("templateCode");
        String title = params.get("title");
        String paramsJson = params.get("params");
        if (templateCode == null || templateCode.isBlank() || title == null || title.isBlank()) {
            throw new IllegalArgumentException("模板编码和标题不能为空");
        }

        SseEmitter emitter = SseStream.createEmitter();
        SseStream.finish(emitter, docGenerationService.generateDocumentStreaming(
                templateCode, title, paramsJson,
                token -> SseStream.sendToken(emitter, token)));
        return emitter;
    }

    @GetMapping("/{id}")
    public R<DocGeneration> getDocument(@PathVariable Long id) {
        return R.success(docGenerationService.getDocument(id));
    }

    @GetMapping("/list")
    public R<List<DocGeneration>> listDocuments() {
        return R.success(docGenerationService.listDocuments());
    }
}