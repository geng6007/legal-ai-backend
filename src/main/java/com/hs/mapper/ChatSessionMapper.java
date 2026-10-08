package com.hs.mapper;

import com.hs.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    int insert(ChatSession session);

    int update(ChatSession session);

    int logicDeleteById(Long id);

    ChatSession selectById(Long id);

    List<ChatSession> selectAll();
}