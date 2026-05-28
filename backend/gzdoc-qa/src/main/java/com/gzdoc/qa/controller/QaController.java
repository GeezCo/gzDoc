package com.gzdoc.qa.controller;

import com.gzdoc.common.result.Result;
import com.gzdoc.qa.dto.QaRequest;
import com.gzdoc.qa.dto.QaResponse;
import com.gzdoc.qa.entity.QaRecord;
import com.gzdoc.qa.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问答控制器
 */
@Tag(name = "智能问答")
@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QaController {

    private final QaService qaService;

    @Operation(summary = "提问")
    @PostMapping("/ask")
    public Result<QaResponse> ask(
            @Valid @RequestBody QaRequest request,
            @Parameter(description = "租户ID") @RequestHeader("X-Tenant-Id") Long tenantId,
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") Long userId
    ) {
        QaResponse response = qaService.ask(request, tenantId, userId);
        return Result.success(response);
    }

    @Operation(summary = "获取问答历史")
    @GetMapping("/history")
    public Result<List<QaRecord>> getHistory(
            @Parameter(description = "租户ID") @RequestHeader("X-Tenant-Id") Long tenantId,
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<QaRecord> history = qaService.getHistory(tenantId, userId, limit);
        return Result.success(history);
    }
}
