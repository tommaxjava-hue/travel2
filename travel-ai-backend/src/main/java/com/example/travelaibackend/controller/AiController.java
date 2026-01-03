package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.service.IAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AiController {

    @Autowired
    private IAiService aiService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    // 3. AI 解析文本
    @PostMapping("/parse")
    public Result<String> parseSpotInfo(@RequestBody Map<String, String> params) {
        String text = params.get("text");
        String prompt = "我将提供一段关于旅游景点的文本，请提取以下信息并返回严格的JSON格式：\n" +
                "1. name (景点名称)\n" +
                "2. city (必须提取所在城市，如'上海'、'北京'。如果文本中未提及，请根据该景点的知名度自行推断补全，严禁返回空值)\n" +
                "3. description (100字以内简介)\n" +
                "4. ticketPrice (数字，免费则为0)\n" +
                "5. openTime\n" +
                "文本内容：[" + text + "]\n" +
                "请严格只返回一个 JSON 字符串，不要包含 Markdown 格式（如 ```json），不要有其他废话。";

        String json = aiService.chat(prompt);
        json = json.replace("```json", "").replace("```", "").trim();
        return Result.success(json);
    }

    // 4. 【核心修改】保存到我的行程
    @PostMapping("/saveItinerary")
    public Result<?> saveItinerary(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        String content = (String) params.get("content"); // 前端传来的 key 叫 content

        if (userId == null || content == null) {
            return Result.error("400", "参数缺失");
        }

        // 自动生成标题：取前10个字
        String title = "AI智能行程";
        if (content.length() > 0) {
            int len = Math.min(content.length(), 10);
            title = content.substring(0, len).replace("\n", "") + "...";
        }

        // 插入 SQL：注意这里 insert into note 字段
        String sql = "INSERT INTO itinerary (user_id, title, note, start_date, end_date, create_time) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            jdbcTemplate.update(sql,
                    userId,
                    title,
                    content, // 将内容存入 note 字段
                    LocalDate.now(), // 开始时间设为今天
                    LocalDate.now().plusDays(1), // 结束时间设为明天
                    LocalDateTime.now()
            );
            return Result.success("已保存到【我的行程计划】");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "保存失败：" + e.getMessage());
        }
    }
}