from typing import List, Dict, Any
from openai import OpenAI
from app.config import get_settings
from app.services.vector_service import VectorService

settings = get_settings()


class QAService:
    """问答服务"""

    def __init__(self):
        self.openai_client = OpenAI(api_key=settings.openai_api_key)
        self.vector_service = VectorService()

    def answer_question(
        self,
        question: str,
        tenant_id: int,
        max_docs: int = 5,
        session_history: List[Dict[str, str]] = None,
    ) -> Dict[str, Any]:
        """基于RAG的问答"""

        # 1. 检索相关文档
        similar_docs = self.vector_service.search_similar_documents(
            query=question,
            tenant_id=tenant_id,
            limit=max_docs,
        )

        # 2. 构建上下文
        context = self._build_context(similar_docs)

        # 3. 构建消息
        messages = []

        # 系统提示
        system_prompt = """你是一个智能文档助手，基于提供的文档内容回答用户问题。
请遵循以下规则：
1. 只根据提供的文档内容回答问题
2. 如果文档中没有相关信息，请明确告知用户
3. 回答要准确、简洁、有条理
4. 可以引用文档中的具体内容
"""
        messages.append({"role": "system", "content": system_prompt})

        # 添加历史对话
        if session_history:
            messages.extend(session_history)

        # 用户问题
        user_message = f"""参考文档：
{context}

用户问题：{question}

请基于上述文档内容回答问题。"""
        messages.append({"role": "user", "content": user_message})

        # 4. 调用OpenAI生成答案
        response = self.openai_client.chat.completions.create(
            model=settings.openai_model,
            messages=messages,
            temperature=0.7,
            max_tokens=1000,
        )

        answer = response.choices[0].message.content

        # 5. 构建相关文档信息
        related_documents = []
        for doc in similar_docs:
            # 提取相关片段（前200字符）
            snippet = doc["content"][:200] + "..." if len(doc["content"]) > 200 else doc["content"]
            related_documents.append({
                "document_id": doc["document_id"],
                "document_name": doc["document_name"],
                "score": doc["score"],
                "snippet": snippet,
            })

        return {
            "answer": answer,
            "related_documents": related_documents,
        }

    def _build_context(self, documents: List[Dict[str, Any]]) -> str:
        """构建上下文"""
        if not documents:
            return "没有找到相关文档。"

        context_parts = []
        for i, doc in enumerate(documents, 1):
            context_parts.append(f"文档{i}：{doc['document_name']}")
            context_parts.append(f"内容：{doc['content'][:500]}...")  # 限制长度
            context_parts.append("")

        return "\n".join(context_parts)
