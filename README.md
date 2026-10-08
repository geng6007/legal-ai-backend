# 知法：法律AI助手后端项目

## 一、项目概述

项目名称：法律AI助手后端（Legal AI Assistant Backend）

项目类型：Spring Boot后端服务 

开发语言：Java 17 

核心框架：Spring Boot 3.5.16 

AI集成：LangChain4j + 通义千问大模型 

数据库：MySQL 8.x 

持久层：MyBatis 3.0.5 

构建工具：Maven

## 二、技术架构

### 1.核心技术栈

编程语言：Java 17

Web框架：Spring Boot 3.5.16

AI框架：LangChain4j 1.20.0

大模型：通义千问 qwen3.8-max

向量模型：text-embedding-v3

数据库：MySQL 8.x

ORM框架：MyBatis 3.0.5

向量存储：InMemoryEmbeddingStore（内存存储）

PDF处理：PDFBox 3.0.5

工具库：Lombok

### 2.项目结构

```
legal-ai-backend/
├── src/main/java/com/hs/
│   ├── common/           # 公共组件
│   │   └── R.java        # 统一API响应体
│   ├── config/           # 配置类
│   │   ├── LLMConfig.java       # LangChain4j配置
│   │   └── MyBatisConfig.java   # MyBatis配置
│   ├── controller/       # 控制器层
│   │   ├── ChatController.java            # 对话管理
│   │   ├── KnowledgeController.java       # 知识库管理
│   │   ├── ConsultController.java         # 咨询向导
│   │   ├── ContractReviewController.java   # 合同审查
│   │   ├── DocGenerationController.java   # 文书生成
│   │   └── CustomerCategoryController.java # 客户分类
│   ├── entity/           # 实体类
│   │   ├── ChatSession.java        # 对话会话
│   │   ├── ChatMessage.java        # 对话消息
│   │   ├── KbDocument.java         # 知识库文档
│   │   ├── ConsultSession.java     # 咨询会话
│   │   ├── ContractReviewTask.java  # 合同审查任务
│   │   ├── DocGeneration.java      # 文书生成记录
│   │   └── CustomerCategory.java    # 客户分类
│   ├── mapper/           # MyBatis映射接口
│   ├── service/          # 服务层
│   │   ├── interface/    # 服务接口
│   │   └── impl/         # 服务实现
│   └── LegalAiBackendApplication.java # 应用启动类
├── src/main/resources/
│   ├── mapper/           # MyBatis XML映射文件
│   ├── application.yaml  # 应用配置
│   └── schema.sql        # 数据库建表脚本
```

## 三、核心功能模块

### 1.智能对话系统

会话管理：支持多轮对话，记录对话历史

消息管理：用户与AI的问答消息存储

流式响应：支持SSE（Server-Sent Events）流式响应

会话摘要：自动生成会话内容摘要

### 2. 知识库管理系统

文档上传：支持文本和PDF文档上传

文档切分：自动将文档切分为片段

语义检索：基于向量嵌入的语义搜索

文档分类：支持法规、案例、合同等分类

### 3. 法律咨询向导

结构化问答：引导式法律咨询流程

场景编码：支持不同法律场景（如劳动法、合同法）

报告生成：自动生成咨询结论报告

进度跟踪：记录咨询进度和用户回答

### 4. 合同审查系统

文件上传：支持合同文件上传 

风险分析：自动识别合同风险条款

条款统计：统计条款总数和风险条款数

审查结果：生成详细的审查结果报告

### 5. 文书生成系统

模板管理：支持多种法律文书模板

内容生成：基于模板和输入生成文书

文件保存：生成文档保存到文件系统

历史记录：记录文书生成历史

## 四、数据库设计

### 核心数据表

chat_session - 对话会话表

chat_message - 对话消息表

kb_document - 知识库文档表

consult_session - 咨询向导会话表

contract_review_task - 合同审查任务表

doc_generation - 文书生成记录表

### 设计特点

逻辑删除：所有表都包含deleted字段支持软删除

时间戳：统一使用create_time和update_time字段

索引优化：针对查询频繁的字段建立索引

外键约束：维护数据完整性

UTF-8编码：支持中文内容存储

## 五、AI集成架构

### LangChain4j配置

```java
@Configuration
public class LLMConfig {
    @Bean
    public ChatModel chatModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen3.8-max")
                .temperature(0.7f)
                .build();
    }
    
    @Bean
    public StreamingChatModel streamingChatModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen3.8-max")
                .temperature(0.7f)
                .build();
    }
    
    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v3")
                .build();
    }
}
```

### RAG（检索增强生成）

向量存储：使用内存向量存储（可扩展为持久化存储）

文档嵌入：将文档内容转换为向量表示

语义检索：基于向量相似度进行文档检索

上下文增强：检索结果作为上下文输入给大模型

## 六、配置管理

### 应用配置（application.yaml）

数据库连接：MySQL 8.x，支持自动创建数据库

文件上传：最大20MB文件上传限制

MyBatis配置：驼峰命名映射，SQL日志输出

LangChain4j配置：通义千问API配置，超时60秒

环境变量：API密钥通过环境变量QIANWEN_API_KEY注入

### 构建配置（pom.xml）

Spring Boot：3.5.16版本

依赖管理：使用LangChain4j BOM管理版本

插件配置：Spring Boot Maven插件打包

开发依赖：Lombok简化代码，PDFBox处理PDF

## 七、开发规范

### 1.代码组织规范

分层架构：Controller-Service-Mapper-Entity四层架构

接口分离：Service层采用接口+Impl实现模式

命名规范：接口以I开头，实现类以Impl结尾

依赖注入：使用构造函数注入

### 2. 时间处理规范

```java
@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private LocalDateTime createTime;
```

统一使用双注解处理时间字段

前端请求参数格式绑定

后端JSON序列化/反序列化

指定时区为中国标准时间

### 3. API响应规范

```java
@Data
public class R<T> {
    private int code;
    private String message;
    private T data;
}
```

统一响应结构：所有API返回统一格式

状态码规范：200成功，400参数错误，500服务器错误

错误信息：明确的错误提示信息

## 八、部署与运行

### 1.环境要求

Java：JDK 17或更高版本

MySQL：8.x版本

Maven：3.6+版本

环境变量：QIANWEN_API_KEY（通义千问API密钥）

### 2.构建命令

```bash
mvn clean compile      # 编译项目
mvn test               # 运行测试
mvn package           # 打包为JAR
mvn spring-boot:run   # 启动应用
```

### 3.数据库初始化

自动建库：连接参数createDatabaseIfNotExist=true

自动建表：启动时自动执行schema.sql脚本

## 九、性能特点

### 1.优势

技术选型合理：Spring Boot + LangChain4j成熟组合

模块化设计：功能模块清晰，易于扩展

AI集成完善：支持对话、检索、生成等多种AI能力

代码规范：遵循良好的开发规范和架构模式

配置灵活：支持环境变量配置，安全性高

### 2.可扩展性

向量存储：当前使用内存存储，可替换为Redis或专用向量数据库

模型切换：LangChain4j抽象层支持切换不同大模型

功能扩展：模块化设计便于添加新功能

微服务化：当前为单体应用，可拆分为微服务架构

## 十、总结

法律AI助手后端是一个功能完善的企业级法律AI应用后端系统，具备以下特点：

1.技术先进性：集成最新的AI技术栈，使用LangChain4j框架

2.功能完整性：涵盖对话、知识库、咨询、审查、生成等全流程

3.架构合理性：遵循MVC分层架构，代码组织清晰

4.开发规范性：严格遵循开发规范，代码质量高

5.可维护性：配置灵活，易于部署和维护

6.扩展性：模块化设计便于功能扩展和性能优化

该项目为法律行业提供了完整的AI解决方案，能够显著提升法律服务的效率和质量，具有良好的应用前景和商业价值。
