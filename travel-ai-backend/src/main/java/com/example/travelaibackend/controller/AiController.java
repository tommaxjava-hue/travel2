package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.service.IAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AiController {

    @Autowired
    private IAiService aiService;

    @PostMapping("/ask")
    public Result<String> ask(@RequestBody Map<String, String> params) {
        String question = params.get("question");
        return Result.success(aiService.chat(question));
    }

    @GetMapping(value = "/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@RequestParam String question, @RequestParam(required = false) Long userId) {
        // 将复杂的流式输出和 Redis 上下文记忆处理，全部转交给 Service 层
        return aiService.streamAsk(question, userId);
    }

    @PostMapping("/plan")
    public Result<String> plan(@RequestBody Map<String, Object> params) {
        String spots = params.get("spots").toString();
        String prompt = "请根据以下景点列表，规划一条合理的游玩路线，仅返回景点名称排序，用逗号分隔：" + spots;
        return Result.success(aiService.chat(prompt));
    }

    @PostMapping("/parse")
    public Result<String> parseSpotInfo(@RequestBody Map<String, String> params) {
        String text = params.get("text");
        String prompt = "提取以下信息并返回JSON(不要Markdown):\n1.name\n2.city\n3.description\n4.ticketPrice\n5.openTime\n6.address(必填)\n文本:[" + text + "]";
        String json = aiService.chat(prompt).replace("```json", "").replace("```", "").trim();
        return Result.success(json);
    }

    @PostMapping("/saveItinerary")
    public Result<?> saveItinerary(@RequestBody Map<String, Object> params) {
        // 使用上一步修复好的安全类型转换逻辑
        Object userIdObj = params.get("userId");
        if (userIdObj == null) {
            return Result.error("400", "用户ID缺失");
        }

        Long userId;
        if (userIdObj instanceof Integer) {
            userId = ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            userId = (Long) userIdObj;
        } else {
            userId = Long.parseLong(userIdObj.toString());
        }

        String content = (String) params.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Result.error("400", "行程内容不能为空");
        }

        try {
            aiService.saveItinerary(userId, content);
            return Result.success("已保存到【我的行程计划】");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "保存失败：" + e.getMessage());
        }
    }

    @GetMapping("/summary/{spotId}")
    public Result<String> getSpotSummary(@PathVariable Integer spotId) {
        String summary = aiService.generateSpotSummary(spotId);
        return Result.success(summary);
    }
}