package com.hs.controller;

import com.hs.common.R;
import com.hs.common.SseStream;
import com.hs.entity.ContractReviewTask;
import com.hs.service.IContractReviewService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 合同审查接口
 */
@RestController
@RequestMapping("/api/contract")
public class ContractReviewController {

    private final IContractReviewService contractReviewService;

    public ContractReviewController(IContractReviewService contractReviewService) {
        this.contractReviewService = contractReviewService;
    }

    @PostMapping("/review")
    public R<ContractReviewTask> createReview(@RequestBody Map<String, String> params) {
        String fileName = params.get("fileName");
        String content = params.get("content");
        if (fileName == null || content == null) {
            return R.error(400, "文件名和合同内容不能为空");
        }
        return R.success(contractReviewService.createTask(fileName, content));
    }

    @PostMapping(value = "/review/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createReviewStreaming(@RequestBody Map<String, String> params) {
        String fileName = params.get("fileName");
        String content = params.get("content");
        if (fileName == null || content == null || content.isBlank()) {
            throw new IllegalArgumentException("文件名和合同内容不能为空");
        }

        ContractReviewTask task = contractReviewService.createStreamingTask(fileName);
        SseEmitter emitter = SseStream.createEmitter();
        SseStream.sendStart(emitter, task);
        SseStream.finish(emitter, contractReviewService.reviewContractStreaming(
                task, content, token -> SseStream.sendToken(emitter, token)));
        return emitter;
    }

    @GetMapping("/review/{id}")
    public R<ContractReviewTask> getReview(@PathVariable Long id) {
        return R.success(contractReviewService.getTask(id));
    }

    @GetMapping("/reviews")
    public R<List<ContractReviewTask>> listReviews() {
        return R.success(contractReviewService.listTasks());
    }
}