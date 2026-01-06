package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.service.IAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AiController {

    @Autowired
    private IAiService aiService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 1. 普通聊天接口
    @PostMapping("/ask")
    public Result<String> ask(@RequestBody Map<String, String> params) {
        String question = params.get("question");
        return Result.success(aiService.chat(question));
    }

    // 2. 流式输出接口 (SSE)
    @GetMapping(value = "/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@RequestParam String question, @RequestParam(required = false) Long userId) {
        // 60秒超时
        SseEmitter emitter = new SseEmitter(60000L);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. 获取上下文
                String historyKey = "chat:history:" + (userId != null ? userId : "guest");
                String history = redisTemplate.opsForValue().get(historyKey);
                String fullPrompt = (history != null ? history + "\n" : "") + "User: " + question;

                // 2. 调用 AI
                System.out.println(">>> AI 思考中: " + question);
                String response = aiService.chat(fullPrompt);
                System.out.println(">>> AI 回复: " + response);

                // 3. 更新上下文
                redisTemplate.opsForValue().set(historyKey, fullPrompt + "\nAI: " + response, 30, TimeUnit.MINUTES);

                // 4. 流式推送
                for (char c : response.toCharArray()) {
                    // 🔥 使用标准 data: 格式发送
                    emitter.send(SseEmitter.event().data(String.valueOf(c)));
                    Thread.sleep(30);
                }
                emitter.complete();
            } catch (Exception e) {
                e.printStackTrace(); // 打印后端报错
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    // ... 其他接口保持不变 (/plan, /parse, /saveItinerary)
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
        Integer userId = (Integer) params.get("userId");
        String content = (String) params.get("content");
        if (userId == null || content == null) return Result.error("400", "参数缺失");

        String title = "AI智能行程";
        if (content.length() > 0) {
            int len = Math.min(content.length(), 10);
            title = content.substring(0, len).replace("\n", "") + "...";
        }

        try {
            jdbcTemplate.update("INSERT INTO itinerary (user_id, title, note, start_date, end_date, create_time) VALUES (?,?,?,?,?,NOW())",
                    userId, title, content, LocalDate.now(), LocalDate.now().plusDays(1));
            return Result.success("已保存到【我的行程计划】");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "保存失败");
        }
    }
}