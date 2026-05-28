package com.gzdoc.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档响应DTO
 */
@Data
@Schema(description = "文档响应")
public class DocumentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long id;

    @Schema(description = "文档名称")
    private String name;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文档状态")
    private String status;

    @Schema(description = "向量化状态")
    private Integer vectorized;

    @Schema(description = "上传用户ID")
    private Long uploadUserId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
