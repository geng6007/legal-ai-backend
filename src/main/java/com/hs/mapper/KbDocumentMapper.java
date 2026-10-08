package com.hs.mapper;

import com.hs.entity.KbDocument;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KbDocumentMapper {

    int insert(KbDocument document);

    int update(KbDocument document);

    int logicDeleteById(Long id);

    KbDocument selectById(Long id);

    List<KbDocument> selectAll();

    List<KbDocument> selectByDocType(String docType);
}