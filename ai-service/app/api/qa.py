from fastapi import APIRouter, HTTPException
from app.models.schemas import QARequest, QAResponse, RelatedDocument
from app.services.qa_service import QAService
import uuid
import time

router = APIRouter()
qa_service = QAService()

# 会话存储（生产环境应使用Redis）
sessions = {}


@router.post("/ask", response_model=QAResponse)
async def ask_question(request: QARequest):
    """问答接口"""
    try:
        # 获取会话历史
        session_history = None
        if request.session_id and request.session_id in sessions:
            session_history = sessions[request.session_id]

        # 调用问答服务
        result = qa_service.answer_question(
            question=request.question,
            tenant_id=request.tenant_id,
            max_docs=request.max_docs,
            session_history=session_history,
        )

        # 生成或使用现有会话ID
        session_id = request.session_id or str(uuid.uuid4())

        # 更新会话历史
        if session_id not in sessions:
            sessions[session_id] = []

        sessions[session_id].append({"role": "user", "content": request.question})
        sessions[session_id].append({"role": "assistant", "content": result["answer"]})

        # 限制会话历史长度
        if len(sessions[session_id]) > 10:
            sessions[session_id] = sessions[session_id][-10:]

        # 构建响应
        related_documents = [
            RelatedDocument(**doc) for doc in result["related_documents"]
        ]

        response = QAResponse(
            question=request.question,
            answer=result["answer"],
            related_documents=related_documents,
            session_id=session_id,
        )

        return response

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"问答失败: {str(e)}")


@router.delete("/session/{session_id}")
async def clear_session(session_id: str):
    """清除会话"""
    if session_id in sessions:
        del sessions[session_id]
        return {"message": "会话已清除"}
    else:
        raise HTTPException(status_code=404, detail="会话不存在")
