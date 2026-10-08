package com.hs.mapper;

import com.hs.entity.ConsultSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConsultSessionMapper {

    int insert(ConsultSession session);

    int update(ConsultSession session);

    int logicDeleteById(Long id);

    ConsultSession selectById(Long id);

    List<ConsultSession> selectAll();
}