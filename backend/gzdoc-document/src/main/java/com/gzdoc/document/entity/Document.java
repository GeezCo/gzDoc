package com.gzdoc.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Data
@TableName("t_document")
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 存储路径
     */
    private String storagePath;

    /**
     * 文档状态：pending-待处理，processing-处理中，completed-已完成，failed-失败
     */
    private String status;

    /**
     * 解析内容
     */
    private String content;

    /**
     * 向量化状态：0-未向量化，1-已向量化
     */
    private Integer vectorized;

    /**
     * 向量ID（Weaviate中的ID）
     */
    private String vectorId;

    /**
     * 上传用户ID
     */
    private Long uploadUserId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标记：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
