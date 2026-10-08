package com.hs.service.impl;

import com.hs.config.SnowflakeIdGenerator;
import com.hs.entity.ChatMessage;
import com.hs.entity.ChatSession;
import com.hs.mapper.ChatMessageMapper;
import com.hs.mapper.ChatSessionMapper;
import com.hs.service.IChatService;
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
 * 对话服务实现类<p>
 * 
 * 功能概述：<p></p>
 * <p>实现 IChatService 接口，提供完整的对话管理功能，包括：</p>
 * <p>- 会话管理（创建、删除、查询）</p>
 * <p>- 消息处理（发送、接收、历史记录）</p>
 * <p>- AI 对话集成（普通对话和流式对话）</p>
 * <p>- 数据持久化（会话和消息存储）</p>
 * <p>设计特点：</p>
 * <p>- 使用 Spring 依赖注入管理依赖</p>
 * <p>- 通过 @Transactional 保证数据一致性</p>
 * <p>- 集成 LangChain4j 框架调用 AI 模型</p>
 * <p>- 支持同步和异步两种对话模式</p>
 */
@Service
public class ChatServiceImpl implements IChatService {

    /**
     * 会话数据访问层<p>
     * 负责 ChatSession 实体的 CRUD 操作，包括：
     * - 插入新会话
     * - 逻辑删除会话
     * - 查询会话列表和详情
     * - 更新会话信息
     */
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    /**
     * ChatModel 的作用<p>
     * ChatModel 是 LangChain4j 框架中与大语言模型（LLM）交互的核心接口：<p>
     * 功能定位：<p>
     * 它是 AI 对话能力的统一抽象接口<p>
     * 负责将输入文本发送给 AI 模型并获取回复<p>
     * 屏蔽了不同 AI 模型（OpenAI、通义千问、本地模型等）的底层实现差异<p>
     * 核心功能：<p>
     * 发送消息给 AI 模型<p>
     * 接收 AI 回复<p>
     * 支持多轮对话上下文<p>
     * 统一处理 API 调用细节<p>
     * 
     * 在这个法律 AI 助手项目中，chatModel 字段用于：<p>
     * 1. 将用户的法律问题发送给通义千问模型<p>
     * 2. 获取基于中国法律法规的专业法律建议<p>
     * 3. 支持多轮对话上下文理解<p>
     * 
     * 配置信息：<p>
     * - AI 提供商：通义千问（qwen3.8-max）<p>
     * - 温度参数：0.7（控制生成文本的随机性）<p>
     * - 超时时间：60秒<p>
     * - API 密钥：从环境变量 QIANWEN_API_KEY 读取<p>
     * 
     * 设计优点：<p>
     * - 抽象性：使用接口编程，便于更换底层 AI 模型<p>
     * - 可测试性：可以通过 Mock 替换进行单元测试<p>
     * - 配置灵活：通过配置文件轻松调整 AI 模型参数<p>
     * - 松耦合：服务层不关心具体的 AI 实现细节<p>
     */
    private final ChatModel chatModel;
    
    /**
     * StreamingChatModel 的作用<p>
     * StreamingChatModel 是 ChatModel 的流式版本，支持实时流式对话：<p>
     * 功能定位：<p>
     * 支持边生成边返回的流式响应<p>
     * 提供类似打字机效果的实时对话体验<p>
     * 适用于需要即时反馈的聊天场景<p>
     * 
     * 核心特点：<p>
     * 1. 实时响应：AI 可以逐词逐句地实时返回部分结果<p>
     * 2. 用户体验：让用户看到 AI 正在思考并逐步生成回答的过程<p>
     * 3. 异步处理：使用回调函数处理流式响应<p>
     * 与 chatModel 的区别：<p>
     * - chatModel：一次性返回完整回复，适合同步调用<p>
     * - streamingChatModel：流式返回，边生成边显示，适合实时交互<p>
     * 在 sendMessageStreaming 方法中使用，为前端提供流式对话体验
     */
    private final StreamingChatModel streamingChatModel;

    /**
     * 雪花算法 ID 生成器<p>
     * 用于生成分布式系统中唯一的 ID，特点：<p>
     * - 全局唯一性：避免 ID 冲突<p>
     * - 有序性：ID 按时间递增<p>
     * - 高性能：本地生成，无需数据库交互<p>
     * 应用场景：<p>
     * - 生成会话 ID<p>
     * - 生成消息 ID<p>
     * - 确保数据主键的唯一性<p>
     */
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 构造器注入 - Spring 依赖注入的标准方式<p>
     * 
     * 工作原理：<p>
     * 1. Spring 容器会自动查找匹配类型的 Bean<p>
     * 2. 创建 ChatServiceImpl 实例时自动注入依赖<p>
     * 3. 确保所有 final 字段在对象创建时被初始化<p>
     * 依赖说明：
     * @param sessionMapper - 会话数据访问层，负责 CRUD 操作
     * @param messageMapper - 消息数据访问层，负责消息管理
     * @param chatModel - 普通 AI 聊天模型（通过 LangChain4j 自动配置）
     * @param streamingChatModel - 流式 AI 聊天模型（通过 LangChain4j 自动配置）
     * @param idGenerator - 雪花算法 ID 生成器，生成唯一 ID
     */
    public ChatServiceImpl(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper,
                           ChatModel chatModel, StreamingChatModel streamingChatModel,
                           SnowflakeIdGenerator idGenerator) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.idGenerator = idGenerator;
    }

    /**
     * 创建新会话<p>
     * 
     * 功能说明：<p>
     * 创建一个新的对话会话，包含标题、场景等信息<p>
     * 执行流程：<p>
     * 1. 创建 ChatSession 实体对象<p>
     * 2. 生成唯一 ID（雪花算法）<p>
     * 3. 设置会话属性（标题、场景、时间等）<p>
     * 4. 设置删除标志为 0（未删除）<p>
     * 5. 插入数据库<p>
     * 6. 返回创建的会话对象<p>
     * 事务说明：
     * 使用 @Transactional 注解，确保操作原子性
     * 
     * @param title - 会话标题，如果为 null 则使用默认标题"新会话"
     * @param scenario - 会话场景描述
     * @return 创建的会话对象
     */
    @Override
    @Transactional
    public ChatSession createSession(String title, String scenario) {
        ChatSession session = new ChatSession();
        session.setId(idGenerator.nextId());
        session.setTitle(title != null ? title : "新会话");
        session.setScenario(scenario);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setDeleted(0);
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 删除会话（逻辑删除）<p>
     * 
      * 功能说明：<p>
      * 对会话进行逻辑删除，而不是物理删除<p>
     * 
      * 设计考虑：<p>
      * - 逻辑删除保留数据完整性<p>
      * - 支持数据恢复和审计<p>
      * - 避免级联删除带来的复杂性<p>
     * 
      * 执行流程：<p>
      * 1. 调用 sessionMapper.logicDeleteById 方法<p>
      * 2. 将 deleted 字段设置为 1<p>
      * 3. 不实际删除数据库记录<p>
     * 
     * 事务说明：
     * 使用 @Transactional 注解，确保操作原子性
     * 
     * @param sessionId - 要删除的会话 ID
     */
    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        sessionMapper.logicDeleteById(sessionId);
    }

    /**
     * 获取会话列表<p>
     * 
     * 功能说明：<p>
     * 查询所有未删除的会话列表<p>
     * 
     * 执行流程：<p>
     * 1. 调用 sessionMapper.selectAll() 方法<p>
     * 2. 返回所有 deleted=0 的会话记录<p>
     * 
     * 性能考虑：<p>
     * - 适合会话数量较少的场景<p>
     * - 如果会话数量大，应考虑分页查询<p>
     * 
     * @return 会话列表
     */
    @Override
    public List<ChatSession> listSessions() {
        return sessionMapper.selectAll();
    }

    /**
     * 获取会话详情<p>
     * 
     * 功能说明：<p>
     * 根据会话 ID 查询会话详细信息<p>
     * 
     * 执行流程：<p>
     * 1. 调用 sessionMapper.selectById() 方法<p>
     * 2. 返回指定 ID 的会话对象<p>
     * 
     * 异常处理：
     * - 如果会话不存在，返回 null
     * - 调用方需要处理 null 值情况
     * 
     * @param sessionId - 会话 ID
     * @return 会话对象，如果不存在返回 null
     */
    @Override
    public ChatSession getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /**
     * 发送消息并获取 AI 回复<p>
     * 
     * 功能说明：<p>
     * 完整的消息处理流程，包括：<p>
     * - 保存用户消息<p>
     * - 构建对话上下文<p>
     * - 调用 AI 模型获取回复<p>
     * - 保存 AI 回复<p>
     * - 更新会话信息<p>
     * 
     * 执行流程：<p>
     * 1. 保存用户消息到数据库<p>
     * 2. 查询会话历史记录<p>
     * 3. 构建完整的对话上下文<p>
     * 4. 调用 AI 模型获取法律咨询回复<p>
     * 5. 保存 AI 回复到数据库<p>
     * 6. 更新会话标题和更新时间<p>
     * 7. 返回 AI 回复消息<p>
     * 
     * 事务说明：
     * 使用 @Transactional 注解，确保整个流程的原子性
     * 如果任何步骤失败，整个操作都会回滚
     * 
     * @param sessionId - 会话 ID
     * @param content - 用户消息内容
     * @return AI 回复消息对象
     */
    @Override
    @Transactional
    public ChatMessage sendMessage(Long sessionId, String content) {
        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setId(idGenerator.nextId());
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setCreateTime(LocalDateTime.now());
        userMsg.setDeleted(0);
        messageMapper.insert(userMsg);

        // 构建对话上下文
        List<ChatMessage> history = messageMapper.selectBySessionId(sessionId);
        StringBuilder contextBuilder = new StringBuilder();
        for (ChatMessage msg : history) {
            String role = "user".equals(msg.getRole()) ? "用户" : "AI";
            contextBuilder.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        
        
        /*
        chatModel 负责：
            接收构建好的对话请求
            调用配置的 AI 模型（这里是通义千问 qwen3.8-max）
            获取完整的法律咨询回复
        
        具体步骤：
        1. 构建专业法律助手提示词
        2. 将对话历史整合到提示词中
        3. 创建 ChatRequest 请求对象
        4. 通过 chatModel.chat() 发送请求
        5. 从响应中提取 AI 回复文本
        
        技术实现：
        - 使用 LangChain4j 的 ChatRequest 构建器
        - 调用 chatModel.chat() 方法发送请求
        - 从 ChatResponse 中提取 aiMessage().text()
        - 所有网络通信、错误处理、重试逻辑都由框架处理
         */
        String prompt = "你是一个专业的法律AI助手，请基于中国法律法规回答以下问题。\n" +
                "对话历史：\n" + contextBuilder +
                "\n请回答用户最新的问题。";

        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .build();
        ChatResponse response = chatModel.chat(request);
        String aiReply = response.aiMessage().text();

        // 保存 AI 回复
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setId(idGenerator.nextId());
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiReply);
        aiMsg.setCreateTime(LocalDateTime.now());
        aiMsg.setDeleted(0);
        messageMapper.insert(aiMsg);

        // 更新会话标题和时间
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            if ("新会话".equals(session.getTitle()) && content.length() <= 50) {
                session.setTitle(content.length() > 20 ? content.substring(0, 20) + "..." : content);
            }
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.update(session);
        }

        return aiMsg;
    }

    /**
     * 流式发送消息<p>
     * 
      * 功能说明：<p>
      * 提供流式对话体验，AI 回复边生成边返回<p>
     * 
      * 执行流程：<p>
      * 1. 保存用户消息<p>
      * 2. 构建对话请求<p>
      * 3. 创建 CompletableFuture 用于异步结果<p>
      * 4. 调用流式 AI 模型<p>
      * 5. 通过回调处理流式响应<p>
      * 6. 最终保存完整回复<p>
     * 
      * 流式响应的三个回调：<p>
      * - onPartialResponse：接收部分响应，通过回调传递给前端<p>
      * - onCompleteResponse：响应完成，保存完整消息并更新会话<p>
      * - onError：处理错误，返回异常结果<p>
     * 
      * 设计优势：<p>
      * - 实时性：用户立即看到 AI 正在思考<p>
      * - 体验好：类似打字机的效果提升用户体验<p>
      * - 资源友好：可以更早地开始处理和显示结果<p>
     * 
     * @param sessionId - 会话 ID
     * @param content - 用户消息内容
     * @param onPartialResponse - 部分响应回调函数
     * @return CompletableFuture<ChatMessage> 异步结果
     */
    @Override
    public CompletableFuture<ChatMessage> sendMessageStreaming(Long sessionId, String content,
                                                               Consumer<String> onPartialResponse) {
        saveUserMessage(sessionId, content);
        ChatRequest request = createChatRequest(sessionId);
        StringBuilder responseBuilder = new StringBuilder();
        CompletableFuture<ChatMessage> result = new CompletableFuture<>();

        try {
            /*
            使用 streamingChatModel 实现流式对话：
            1. 调用 streamingChatModel.chat() 方法，传入请求和回调处理器
            2. 通过 StreamingChatResponseHandler 处理流式响应
            
            流式响应的三个回调：
            - onPartialResponse：接收部分响应，通过回调传递给前端
            - onCompleteResponse：响应完成，保存完整消息并更新会话
            - onError：处理错误，返回异常结果
            
            设计优势：
            - 实时性：用户立即看到 AI 正在思考
            - 体验好：类似打字机的效果提升用户体验
            - 资源友好：可以更早地开始处理和显示结果
            */
            streamingChatModel.chat(request, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    responseBuilder.append(partialResponse);
                    onPartialResponse.accept(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    try {
                        String responseText = responseBuilder.toString();
                        if (responseText.isEmpty() && completeResponse.aiMessage() != null) {
                            responseText = completeResponse.aiMessage().text();
                        }
                        ChatMessage assistantMessage = saveAssistantMessage(sessionId, responseText);
                        updateSessionAfterMessage(sessionId, content);
                        result.complete(assistantMessage);
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
     * 获取会话消息历史<p>
     * 
      * 功能说明：<p>
      * 查询指定会话的所有消息记录<p>
     * 
      * 执行流程：<p>
      * 1. 调用 messageMapper.selectBySessionId() 方法<p>
      * 2. 返回该会话的所有消息列表<p>
     * 
      * 排序说明：<p>
      * - 消息按创建时间升序排列<p>
      * - 确保对话历史的正确顺序<p>
     * 
     * @param sessionId - 会话 ID
     * @return 消息列表
     */
    @Override
    public List<ChatMessage> getMessages(Long sessionId) {
        return messageMapper.selectBySessionId(sessionId);
    }

    /**
     * 保存用户消息（私有方法）<p>
     * 
      * 功能说明：<p>
      * 封装用户消息的保存逻辑，供其他方法复用<p>
     * 
      * 执行流程：<p>
      * 1. 创建 ChatMessage 对象<p>
      * 2. 设置消息属性（ID、会话ID、角色、内容等）<p>
      * 3. 插入数据库<p>
     * 
      * 设计考虑：<p>
      * - 提取公共逻辑，避免代码重复<p>
      * - 提高代码可维护性<p>
     * 
     * @param sessionId - 会话 ID
     * @param content - 消息内容
     */
    private void saveUserMessage(Long sessionId, String content) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setId(idGenerator.nextId());
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setDeleted(0);
        messageMapper.insert(userMessage);
    }

    /**
     * 创建对话请求（私有方法）<p>
     * 
      * 功能说明：<p>
      * 构建 AI 对话请求，包含完整的对话上下文<p>
     * 
      * 执行流程：<p>
      * 1. 查询会话历史记录<p>
      * 2. 构建对话上下文字符串<p>
      * 3. 创建专业法律助手提示词<p>
      * 4. 构建 ChatRequest 对象<p>
     * 
     * 提示词设计：
     * - 明确 AI 角色：专业法律助手
     * - 指定法律依据：中国法律法规
     * - 包含对话历史：支持上下文理解
     * - 明确任务：回答最新问题
     * 
     * @param sessionId - 会话 ID
     * @return ChatRequest 对象
     */
    private ChatRequest createChatRequest(Long sessionId) {
        List<ChatMessage> history = messageMapper.selectBySessionId(sessionId);
        StringBuilder contextBuilder = new StringBuilder();
        for (ChatMessage message : history) {
            String role = "user".equals(message.getRole()) ? "用户" : "AI";
            contextBuilder.append(role).append(": ").append(message.getContent()).append("\n");
        }

        String prompt = "你是一个专业的法律AI助手，请基于中国法律法规回答以下问题。\n" +
                "对话历史：\n" + contextBuilder +
                "\n请回答用户最新的问题。";
        return ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .build();
    }

    /**
     * 保存 AI 回复消息（私有方法）<p>
     * 
      * 功能说明：<p>
      * 封装 AI 回复消息的保存逻辑<p>
     * 
      * 执行流程：<p>
      * 1. 创建 ChatMessage 对象<p>
      * 2. 设置消息属性（ID、会话ID、角色、内容等）<p>
      * 3. 插入数据库<p>
      * 4. 返回保存的消息对象<p>
     * 
     * @param sessionId - 会话 ID
     * @param content - AI 回复内容
     * @return 保存的 AI 消息对象
     */
    private ChatMessage saveAssistantMessage(Long sessionId, String content) {
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setId(idGenerator.nextId());
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(content);
        assistantMessage.setCreateTime(LocalDateTime.now());
        assistantMessage.setDeleted(0);
        messageMapper.insert(assistantMessage);
        return assistantMessage;
    }

    /**
     * 更新会话信息（私有方法）<p>
     * 
      * 功能说明：<p>
      * 在发送消息后更新会话的相关信息<p>
     * 
      * 执行流程：<p>
      * 1. 查询会话对象<p>
      * 2. 如果会话标题是默认标题，则根据用户消息生成新标题<p>
      * 3. 更新会话的更新时间<p>
      * 4. 保存到数据库<p>
     * 
      * 标题生成逻辑：<p>
      * - 只对默认标题"新会话"进行更新<p>
      * - 截取用户消息前20个字符作为标题<p>
      * - 如果消息超过20个字符，添加省略号<p>
     * 
     * @param sessionId - 会话 ID
     * @param content - 用户消息内容
     */
    private void updateSessionAfterMessage(Long sessionId, String content) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        if ("新会话".equals(session.getTitle()) && !content.isBlank()) {
            int titleLength = Math.min(content.length(), 20);
            session.setTitle(content.substring(0, titleLength) +
                    (content.length() > titleLength ? "..." : ""));
        }
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.update(session);
    }
}
