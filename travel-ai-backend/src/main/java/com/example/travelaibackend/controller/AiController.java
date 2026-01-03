package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.service.IAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AiController {

    @Autowired
    private IAiService aiService;

    @Autowired
    private JdbcTemplate jdbcTemplate; // 用于简单存储 AI 笔记

    // 1. 聊天接口
    @PostMapping("/ask")
    public Result<String> ask(@RequestBody Map<String, String> params) {
        String question = params.get("question");
        return Result.success(aiService.chat(question));
    }

    // 2. 智能规划
    @PostMapping("/plan")
    public Result<String> plan(@RequestBody Map<String, Object> params) {
        String spots = params.get("spots").toString();
        String prompt = "请根据以下景点列表，规划一条合理的游玩路线，仅返回景点名称排序，用逗号分隔：" + spots;
        return Result.success(aiService.chat(prompt));
    }

    // 3. AI 解析文本 (解决城市识别问题)
    @PostMapping("/parse")
    public Result<String> parseSpotInfo(@RequestBody Map<String, String> params) {
        String text = params.get("text");
        // 核心修改：Prompt 强制要求提取城市
        String prompt = "我将提供一段关于旅游景点的文本，请提取以下信息并返回严格的JSON格式：\n" +
                "1. name (景点名称)\n" +
                "2. city (必须提取所在城市，如'上海'、'北京'。如果文本中未提及，请根据该景点的知名度自行推断补全，严禁返回空值)\n" +
                "3. description (100字以内简介)\n" +
                "4. ticketPrice (数字，免费则为0)\n" +
                "5. openTime\n" +
                "文本内容：[" + text + "]\n" +
                "请严格只返回一个 JSON 字符串，不要包含 Markdown 格式（如 ```json），不要有其他废话。";

        String json = aiService.chat(prompt);
        // 清洗 Markdown 标记
        json = json.replace("```json", "").replace("```", "").trim();
        return Result.success(json);
    }

    // 4. 收藏 AI 回答 (解决收藏功能)
    @PostMapping("/saveNote")
    public Result<?> saveNote(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        String content = (String) params.get("content");

        if (userId == null || content == null) {
            return Result.error("400", "参数缺失");
        }

        // 插入数据库
        String sql = "INSERT INTO sys_ai_note (user_id, content, create_time) VALUES (?, ?, NOW())";
        try {
            jdbcTemplate.update(sql, userId, content);
            return Result.success("已收藏到笔记");
        } catch (Exception e) {
            return Result.error("500", "收藏失败：" + e.getMessage());
        }
    }
}