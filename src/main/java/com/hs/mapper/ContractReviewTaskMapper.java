package com.hs.mapper;

import com.hs.entity.ContractReviewTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ContractReviewTaskMapper {

    int insert(ContractReviewTask task);

    int update(ContractReviewTask task);

    int logicDeleteById(Long id);

    ContractReviewTask selectById(Long id);

    List<ContractReviewTask> selectAll();
}