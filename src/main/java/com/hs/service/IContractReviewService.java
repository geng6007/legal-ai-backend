package com.hs.service;

import com.hs.entity.ContractReviewTask;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 合同审查服务接口
 */
public interface IContractReviewService {

    /**
     * 创建合同审查任务
     */
    ContractReviewTask createTask(String fileName, String contractContent);

    /**
     * 创建流式合同审查任务
     */
    ContractReviewTask createStreamingTask(String fileName);

    /**
     * 流式审查合同
     */
    CompletableFuture<ContractReviewTask> reviewContractStreaming(
            ContractReviewTask task, String contractContent, Consumer<String> onPartialResponse);

    /**
     * 获取审查任务
     */
    ContractReviewTask getTask(Long taskId);

    /**
     * 获取任务列表
     */
    List<ContractReviewTask> listTasks();
}
