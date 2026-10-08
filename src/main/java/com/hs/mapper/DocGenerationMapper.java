package com.hs.mapper;

import com.hs.entity.DocGeneration;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DocGenerationMapper {

    int insert(DocGeneration record);

    int update(DocGeneration record);

    int logicDeleteById(Long id);

    DocGeneration selectById(Long id);

    List<DocGeneration> selectAll();
}