package com.gzdoc.document.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gzdoc.common.exception.BusinessException;
import com.gzdoc.document.dto.DocumentResponse;
import com.gzdoc.document.entity.Document;
import com.gzdoc.document.mapper.DocumentMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final MinioClient minioClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${minio.bucket-name:gzdoc}")
    private String bucketName;

    /**
     * 上传文档
     */
    public DocumentResponse upload(MultipartFile file, Long tenantId, Long userId) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                throw new BusinessException(400, "文件不能为空");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new BusinessException(400, "文件名不能为空");
            }

            // 获取文件扩展名
            String fileType = FileUtil.extName(originalFilename);
            long fileSize = file.getSize();

            // 生成存储路径
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = IdUtil.simpleUUID() + "." + fileType;
            String storagePath = datePath + "/" + fileName;

            // 上传到MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(storagePath)
                                .stream(inputStream, fileSize, -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            // 保存文档记录
            Document document = new Document();
            document.setTenantId(tenantId);
            document.setName(originalFilename);
            document.setFileType(fileType);
            document.setFileSize(fileSize);
            document.setStoragePath(storagePath);
            document.setStatus("pending");
            document.setVectorized(0);
            document.setUploadUserId(userId);

            documentMapper.insert(document);

            // 发送Kafka消息触发文档解析
            Map<String, Object> message = new HashMap<>();
            message.put("documentId", document.getId());
            message.put("storagePath", storagePath);
            message.put("fileType", fileType);
            kafkaTemplate.send("document-parse", document.getId().toString(), message.toString());

            log.info("文档上传成功: {}, ID: {}", originalFilename, document.getId());

            // 转换为响应DTO
            DocumentResponse response = new DocumentResponse();
            BeanUtils.copyProperties(document, response);
            return response;

        } catch (Exception e) {
            log.error("文档上传失败", e);
            throw new BusinessException(500, "文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询文档
     */
    public Page<DocumentResponse> page(Long tenantId, Integer pageNum, Integer pageSize) {
        Page<Document> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getTenantId, tenantId);
        wrapper.orderByDesc(Document::getCreateTime);

        Page<Document> documentPage = documentMapper.selectPage(page, wrapper);

        // 转换为响应DTO
        Page<DocumentResponse> responsePage = new Page<>();
        BeanUtils.copyProperties(documentPage, responsePage, "records");

        List<DocumentResponse> records = documentPage.getRecords().stream()
                .map(doc -> {
                    DocumentResponse response = new DocumentResponse();
                    BeanUtils.copyProperties(doc, response);
                    return response;
                })
                .collect(Collectors.toList());

        responsePage.setRecords(records);
        return responsePage;
    }

    /**
     * 获取文档详情
     */
    public DocumentResponse getById(Long id) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }

        DocumentResponse response = new DocumentResponse();
        BeanUtils.copyProperties(document, response);
        return response;
    }

    /**
     * 删除文档
     */
    public void delete(Long id) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }

        documentMapper.deleteById(id);
        log.info("文档删除成功: ID={}", id);
    }
}
