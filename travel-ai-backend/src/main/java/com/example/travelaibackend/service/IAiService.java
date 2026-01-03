package com.example.travelaibackend.service;

public interface IAiService {
    /**
     * 智能问答
     * @param userQuestion 用户的问题
     * @return AI 的回答
     */
    String chat(String userQuestion);
}