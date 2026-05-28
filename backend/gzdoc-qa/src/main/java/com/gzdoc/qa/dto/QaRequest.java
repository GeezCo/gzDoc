package com.gzdoc.qa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 问答请求DTO
 */
@Data
@Schema(description = "问答请求")
public class QaRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "问题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问题不能为空")
    private String question;

    @Schema(description = "会话ID（用于上下文关联）")
    private String sessionId;

    @Schema(description = "最大返回文档数")
    private Integer maxDocs = 5;
}
