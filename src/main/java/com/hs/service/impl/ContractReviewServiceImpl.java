package com.hs.service.impl;

import com.hs.config.SnowflakeIdGenerator;
import com.hs.entity.ContractReviewTask;
import com.hs.mapper.ContractReviewTaskMapper;
import com.hs.service.IContractReviewService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 合同审查服务实现
 */
@Service
public class ContractReviewServiceImpl implements IContractReviewService {

    private final ContractReviewTaskMapper taskMapper;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final SnowflakeIdGenerator idGenerator;

    public ContractReviewServiceImpl(ContractReviewTaskMapper taskMapper,
                                     ChatModel chatModel,
                                     StreamingChatModel streamingChatModel,
                                     SnowflakeIdGenerator idGenerator) {
        this.taskMapper = taskMapper;
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.idGenerator = idGenerator;
    }

    /**
     * 创建合同审查任务
     */
    @Override
    @Transactional
    public ContractReviewTask createTask(String fileName, String contractContent) {
        ContractReviewTask task = new ContractReviewTask();
        task.setId(idGenerator.nextId());
        task.setFileName(fileName);
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());
        task.setDeleted(0);
        taskMapper.insert(task);

        // 异步执行审查
        reviewContract(task, contractContent);

        return task;
    }

    @Override
    @Transactional
    public ContractReviewTask createStreamingTask(String fileName) {
        ContractReviewTask task = new ContractReviewTask();
        task.setId(idGenerator.nextId());
        task.setFileName(fileName);
        task.setClauseCount(0);
        task.setRiskCount(0);
        task.setStatus("ANALYZING");
        task.setCreateTime(LocalDateTime.now());
        task.setDeleted(0);
        taskMapper.insert(task);
        return task;
    }

    @Override
    public CompletableFuture<ContractReviewTask> reviewContractStreaming(
            ContractReviewTask task, String contractContent, Consumer<String> onPartialResponse) {
        String prompt = "你是一个专业的合同审查律师。请审查以下合同内容，找出风险条款并给出修改建议。\n\n" +
                "合同内容：\n" + contractContent + "\n\n" +
                "请以JSON格式返回结果，包含：\n" +
                "1. clause_count: 总条款数\n" +
                "2. risk_count: 风险条款数\n" +
                "3. clauses: 条款列表，每个条款包含 clause_no(编号), content(内容), risk_level(风险等级), analysis(分析), suggestion(修改建议)";
        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .build();
        StringBuilder resultBuilder = new StringBuilder();
        CompletableFuture<ContractReviewTask> result = new CompletableFuture<>();

        try {
            streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    resultBuilder.append(partialResponse);
                    onPartialResponse.accept(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    try {
                        String reviewResult = resultBuilder.toString();
                        task.setClauseCount(extractInt(reviewResult, "clause_count"));
                        task.setRiskCount(extractInt(reviewResult, "risk_count"));
                        task.setResult(reviewResult);
                        task.setStatus("DONE");
                        taskMapper.update(task);
                        result.complete(task);
                    } catch (RuntimeException exception) {
                        result.completeExceptionally(exception);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    try {
                        task.setStatus("FAILED");
                        task.setResult("审查失败: " + error.getMessage());
                        taskMapper.update(task);
                    } catch (RuntimeException persistenceError) {
                        error.addSuppressed(persistenceError);
                    }
                    result.completeExceptionally(error);
                }
            });
        } catch (RuntimeException exception) {
            try {
                task.setStatus("FAILED");
                task.setResult("审查失败: " + exception.getMessage());
                taskMapper.update(task);
            } catch (RuntimeException persistenceError) {
                exception.addSuppressed(persistenceError);
            }
            result.completeExceptionally(exception);
        }
        return result;
    }

    /**
     * 执行合同审查
     */
    private void reviewContract(ContractReviewTask task, String contractContent) {
        task.setStatus("ANALYZING");
        taskMapper.update(task);

        try {
            String prompt = "你是一个专业的合同审查律师。请审查以下合同内容，找出风险条款并给出修改建议。\n\n" +
                    "合同内容：\n" + contractContent + "\n\n" +
                    "请以JSON格式返回结果，包含：\n" +
                    "1. clause_count: 总条款数\n" +
                    "2. risk_count: 风险条款数\n" +
                    "3. clauses: 条款列表，每个条款包含 clause_no(编号), content(内容), risk_level(风险等级), analysis(分析), suggestion(修改建议)";

            ChatRequest request = ChatRequest.builder()
                    .messages(UserMessage.from(prompt))
                    .build();
            ChatResponse response = chatModel.chat(request);
            String result = response.aiMessage().text();

            // 提取条款数和风险数
            int clauseCount = extractInt(result, "clause_count");
            int riskCount = extractInt(result, "risk_count");

            task.setClauseCount(clauseCount);
            task.setRiskCount(riskCount);
            task.setResult(result);
            task.setStatus("DONE");
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setResult("审查失败: " + e.getMessage());
        }
        taskMapper.update(task);
    }

    /**
     * 获取审查任务
     */
    @Override
    public ContractReviewTask getTask(Long taskId) {
        return taskMapper.selectById(taskId);
    }

    /**
     * 获取任务列表
     */
    @Override
    public List<ContractReviewTask> listTasks() {
        return taskMapper.selectAll();
    }

    private int extractInt(String text, String key) {
        try {
            int idx = text.indexOf(key);
            if (idx < 0) return 0;
            idx = text.indexOf(":", idx);
            if (idx < 0) return 0;
            StringBuilder num = new StringBuilder();
            for (int i = idx + 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (Character.isDigit(c)) {
                    num.append(c);
                } else if (num.length() > 0) {
                    break;
                }
            }
            return num.length() > 0 ? Integer.parseInt(num.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
