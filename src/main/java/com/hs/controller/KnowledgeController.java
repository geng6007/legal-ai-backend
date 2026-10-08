package com.hs.controller;

import com.hs.common.R;
import com.hs.entity.KbDocument;
import com.hs.service.IKnowledgeService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理接口
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final IKnowledgeService knowledgeService;

    public KnowledgeController(IKnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/document")
    public R<KbDocument> addDocument(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        String source = params.get("source");
        String docType = params.get("docType");
        String content = params.get("content");
        if (title == null || content == null) {
            return R.error(400, "标题和内容不能为空");
        }
        return R.success(knowledgeService.addDocument(title, source, docType, content));
    }

    @PostMapping(value = "/document/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<KbDocument> addPdfDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "law") String docType) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的 PDF 文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("仅支持 PDF 文件");
        }
        String documentTitle = title == null || title.isBlank()
                ? originalFilename.substring(0, originalFilename.length() - 4)
                : title.trim();
        return R.success(knowledgeService.addPdfDocument(
                documentTitle, source, docType, file.getBytes()));
    }

    @DeleteMapping("/document/{id}")
    public R<Void> deleteDocument(@PathVariable Long id) {
        knowledgeService.deleteDocument(id);
        return R.success();
    }

    @GetMapping("/document/{id}")
    public R<KbDocument> getDocument(@PathVariable Long id) {
        return R.success(knowledgeService.getDocument(id));
    }

    @GetMapping("/documents")
    public R<List<KbDocument>> listDocuments(@RequestParam(required = false) String docType) {
        return R.success(knowledgeService.listDocuments(docType));
    }

    @GetMapping("/search")
    public R<List<String>> search(@RequestParam String query,
                                  @RequestParam(defaultValue = "5") int maxResults) {
        return R.success(knowledgeService.searchRelevant(query, maxResults));
    }
}