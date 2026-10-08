package com.hs.mapper;

import com.hs.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    List<ChatMessage> selectBySessionId(Long sessionId);

    ChatMessage selectById(Long id);
}