package com.hs.service.impl;

import com.hs.config.SnowflakeIdGenerator;
import com.hs.entity.DocGeneration;
import com.hs.mapper.DocGenerationMapper;
import com.hs.service.IDocGenerationService;
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
 * 文书生成服务实现
 */
@Service
public class DocGenerationServiceImpl implements IDocGenerationService {

    private final DocGenerationMapper generationMapper;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final SnowflakeIdGenerator idGenerator;

    public DocGenerationServiceImpl(DocGenerationMapper generationMapper,
                                    ChatModel chatModel,
                                    StreamingChatModel streamingChatModel,
                                    SnowflakeIdGenerator idGenerator) {
        this.generationMapper = generationMapper;
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.idGenerator = idGenerator;
    }

    /**
     * 生成法律文书
     */
    @Override
    @Transactional
    public DocGeneration generateDocument(String templateCode, String title, String params) {
        DocGeneration record = new DocGeneration();
        record.setId(idGenerator.nextId());
        record.setTemplateCode(templateCode);
        record.setTitle(title);
        record.setCreateTime(LocalDateTime.now());
        record.setDeleted(0);

        // 调用大模型生成文书内容
        String prompt = "你是一个专业的法律文书生成助手。请根据以下模板和参数生成法律文书。\n" +
                "模板编码：" + templateCode + "\n" +
                "文书标题：" + title + "\n" +
                "参数信息：" + params + "\n\n" +
                "请生成完整的法律文书内容，包括标题、当事人信息、正文、落款等。";

        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .build();
        ChatResponse response = chatModel.chat(request);
        String content = response.aiMessage().text();

        record.setContent(content);
        generationMapper.insert(record);
        return record;
    }

    @Override
    public CompletableFuture<DocGeneration> generateDocumentStreaming(
            String templateCode, String title, String params, Consumer<String> onPartialResponse) {
        String prompt = "你是一个专业的法律文书生成助手。请根据以下模板和参数生成法律文书。\n" +
                "模板编码：" + templateCode + "\n" +
                "文书标题：" + title + "\n" +
                "参数信息：" + params + "\n\n" +
                "请生成完整的法律文书内容，包括标题、当事人信息、正文、落款等。";
        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .build();
        StringBuilder contentBuilder = new StringBuilder();
        CompletableFuture<DocGeneration> result = new CompletableFuture<>();

        try {
            streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    contentBuilder.append(partialResponse);
                    onPartialResponse.accept(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    try {
                        DocGeneration record = new DocGeneration();
                        record.setId(idGenerator.nextId());
                        record.setTemplateCode(templateCode);
                        record.setTitle(title);
                        record.setContent(contentBuilder.toString());
                        record.setCreateTime(LocalDateTime.now());
                        record.setDeleted(0);
                        generationMapper.insert(record);
                        result.complete(record);
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
     * 获取文书记录
     */
    @Override
    public DocGeneration getDocument(Long id) {
        return generationMapper.selectById(id);
    }

    /**
     * 获取文书列表
     */
    @Override
    public List<DocGeneration> listDocuments() {
        return generationMapper.selectAll();
    }
}
