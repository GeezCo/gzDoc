package com.gzdoc.document.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gzdoc.common.result.Result;
import com.gzdoc.document.dto.DocumentResponse;
import com.gzdoc.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档控制器
 */
@Tag(name = "文档管理")
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "上传文档")
    @PostMapping("/upload")
    public Result<DocumentResponse> upload(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "租户ID") @RequestHeader("X-Tenant-Id") Long tenantId,
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") Long userId
    ) {
        DocumentResponse response = documentService.upload(file, tenantId, userId);
        return Result.success(response);
    }

    @Operation(summary = "分页查询文档")
    @GetMapping("/page")
    public Result<Page<DocumentResponse>> page(
            @Parameter(description = "租户ID") @RequestHeader("X-Tenant-Id") Long tenantId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<DocumentResponse> page = documentService.page(tenantId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取文档详情")
    @GetMapping("/{id}")
    public Result<DocumentResponse> getById(
            @Parameter(description = "文档ID") @PathVariable Long id
    ) {
        DocumentResponse response = documentService.getById(id);
        return Result.success(response);
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "文档ID") @PathVariable Long id
    ) {
        documentService.delete(id);
        return Result.success();
    }
}
