package com.example.travelaibackend.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IAiService {

    /**
     * 智能问答 (保留原有的基础问答与工具方法能力)
     * @param userQuestion 用户的问题或拼接好的 Prompt
     * @return AI 的回答
     */
    String chat(String userQuestion);

    /**
     * 流式智能导游问答 (SSE推送)
     * @param question 用户的问题
     * @param userId 用户ID (用于关联 Redis 上下文记忆)
     * @return SseEmitter 流式响应对象
     */
    SseEmitter streamAsk(String question, Long userId);

    /**
     * 将 AI 规划的行程直接保存到用户个人计划库
     * @param userId 用户ID
     * @param content 行程的具体内容
     */
    void saveItinerary(Long userId, String content);

    /**
     * 生成并获取景点的 AI 智能口碑总结
     * @param spotId 景点ID
     * @return AI 总结文本
     */
    String generateSpotSummary(Integer spotId);
}