package com.gzdoc.qa.service;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.gzdoc.common.exception.BusinessException;
import com.gzdoc.qa.dto.QaRequest;
import com.gzdoc.qa.dto.QaResponse;
import com.gzdoc.qa.entity.QaRecord;
import com.gzdoc.qa.mapper.QaRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 问答服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaService {

    private final QaRecordMapper qaRecordMapper;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 问答
     */
    public QaResponse ask(QaRequest request, Long tenantId, Long userId) {
        long startTime = System.currentTimeMillis();

        try {
            // 调用AI服务进行问答
            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("question", request.getQuestion());
            aiRequest.put("tenant_id", tenantId);
            aiRequest.put("max_docs", request.getMaxDocs());
            if (request.getSessionId() != null) {
                aiRequest.put("session_id", request.getSessionId());
            }

            String requestBody = JSON.toJSONString(aiRequest);
            Request httpRequest = new Request.Builder()
                    .url(aiServiceUrl + "/api/qa/ask")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            Response httpResponse = httpClient.newCall(httpRequest).execute();
            if (!httpResponse.isSuccessful()) {
                throw new BusinessException(500, "AI服务调用失败: " + httpResponse.code());
            }

            String responseBody = httpResponse.body().string();
            Map<String, Object> aiResponse = JSON.parseObject(responseBody, Map.class);

            // 解析响应
            String answer = (String) aiResponse.get("answer");
            String sessionId = (String) aiResponse.getOrDefault("session_id", IdUtil.simpleUUID());
            List<Map<String, Object>> relatedDocs = (List<Map<String, Object>>) aiResponse.get("related_documents");

            // 构建响应
            List<QaResponse.RelatedDocument> relatedDocuments = new ArrayList<>();
            List<Long> relatedDocIds = new ArrayList<>();

            if (relatedDocs != null) {
                for (Map<String, Object> doc : relatedDocs) {
                    Long docId = Long.valueOf(doc.get("document_id").toString());
                    relatedDocIds.add(docId);

                    QaResponse.RelatedDocument relatedDoc = new QaResponse.RelatedDocument(
                            docId,
                            (String) doc.get("document_name"),
                            (Double) doc.get("score"),
                            (String) doc.get("snippet")
                    );
                    relatedDocuments.add(relatedDoc);
                }
            }

            long responseTime = System.currentTimeMillis() - startTime;

            // 保存问答记录
            QaRecord record = new QaRecord();
            record.setTenantId(tenantId);
            record.setUserId(userId);
            record.setQuestion(request.getQuestion());
            record.setAnswer(answer);
            record.setRelatedDocIds(JSON.toJSONString(relatedDocIds));
            record.setResponseTime(responseTime);
            qaRecordMapper.insert(record);

            log.info("问答完成: 问题={}, 响应时间={}ms", request.getQuestion(), responseTime);

            return new QaResponse(
                    request.getQuestion(),
                    answer,
                    relatedDocuments,
                    responseTime,
                    sessionId
            );

        } catch (IOException e) {
            log.error("AI服务调用失败", e);
            throw new BusinessException(500, "AI服务调用失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("问答处理失败", e);
            throw new BusinessException(500, "问答处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取问答历史
     */
    public List<QaRecord> getHistory(Long tenantId, Long userId, Integer limit) {
        return qaRecordMapper.selectList(null).stream()
                .filter(r -> r.getTenantId().equals(tenantId) && r.getUserId().equals(userId))
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .limit(limit != null ? limit : 10)
                .collect(Collectors.toList());
    }
}
