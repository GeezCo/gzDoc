package com.gzdoc.qa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 问答响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "问答响应")
public class QaResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "问题")
    private String question;

    @Schema(description = "答案")
    private String answer;

    @Schema(description = "相关文档列表")
    private List<RelatedDocument> relatedDocuments;

    @Schema(description = "响应时间（毫秒）")
    private Long responseTime;

    @Schema(description = "会话ID")
    private String sessionId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "相关文档")
    public static class RelatedDocument implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "文档ID")
        private Long documentId;

        @Schema(description = "文档名称")
        private String documentName;

        @Schema(description = "相关度分数")
        private Double score;

        @Schema(description = "相关片段")
        private String snippet;
    }
}
