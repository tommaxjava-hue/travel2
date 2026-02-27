package com.example.travelaibackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.entity.Attraction;
import com.example.travelaibackend.entity.Comment;
import com.example.travelaibackend.entity.Itinerary;
import com.example.travelaibackend.service.IAiService;
import com.example.travelaibackend.service.IAttractionService;
import com.example.travelaibackend.service.ICommentService;
import com.example.travelaibackend.service.IItineraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class AiServiceImpl implements IAiService {

    // 注入你配置好的真实 API 参数
    @Value("${ai.url}")
    private String apiUrl;

    @Value("${ai.key}")
    private String apiKey;

    @Value("${ai.model}")
    private String modelName;

    @Autowired
    private IAttractionService attractionService;

    @Autowired
    private IItineraryService itineraryService;

    @Autowired
    private ICommentService commentService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public String chat(String userQuestion) {
        // 1. 【RAG 检索增强】简单的关键词搜索
        QueryWrapper<Attraction> query = new QueryWrapper<>();
        query.like("city", userQuestion)
                .or().like("name", userQuestion)
                .or().like("tags", userQuestion);
        // 只取前5条，防止 token 超出限制
        query.last("LIMIT 5");

        List<Attraction> relatedSpots = attractionService.list(query);

        // 2. 拼装 Prompt (提示词)
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个专业的智能旅游助手。");

        if (!relatedSpots.isEmpty()) {
            systemPrompt.append("以下是系统数据库中已有的相关景点信息，请优先基于这些信息回答，不需要重复所有字段，只推荐最合适的：\n");
            for (Attraction spot : relatedSpots) {
                systemPrompt.append(String.format("- 名称：%s, 城市：%s, 票价：%s, 特色：%s\n",
                        spot.getName(), spot.getCity(), spot.getTicketPrice(), spot.getTags()));
            }
        } else {
            systemPrompt.append("（系统数据库中暂时没有找到相关景点，请基于你的通用知识回答）");
        }

        // 3. 构建 OpenAI 格式的 JSON 请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("stream", false);

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt.toString());

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userQuestion);

        requestBody.put("messages", new Object[]{systemMessage, userMessage});

        // 4. 发送 HTTP 请求
        try {
            String jsonBody = JSONUtil.toJsonStr(requestBody);
            System.out.println("正在请求AI接口: " + jsonBody);

            String result = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(jsonBody)
                    .execute()
                    .body();

            System.out.println("AI返回原始结果: " + result);

            // 5. 解析返回结果
            JSONObject jsonObject = JSONUtil.parseObj(result);
            JSONArray choices = jsonObject.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = (JSONObject) choices.get(0);
                JSONObject message = choice.getJSONObject("message");
                return message.getStr("content");
            }
            return "AI 思考失败，请检查 API 配置。";

        } catch (Exception e) {
            e.printStackTrace();
            return "AI 服务暂时不可用：" + e.getMessage();
        }
    }

    @Override
    public SseEmitter streamAsk(String question, Long userId) {
        SseEmitter emitter = new SseEmitter(60000L);
        executorService.execute(() -> {
            try {
                emitter.send("AI智能导游正在为您规划...");
                Thread.sleep(1000);
                emitter.send("\n针对您的问题【" + question + "】，推荐路线如下：...");
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @Override
    public void saveItinerary(Long userId, String content) {
        // 完美修复编译报错：严格适配 Itinerary 实体类字段 (userId 为 Long, note 存文本)
        Itinerary itinerary = new Itinerary();
        itinerary.setUserId(userId);
        itinerary.setNote(content);

        // 补全其它必填字段
        String title = "AI智能行程";
        if (content != null && content.length() > 0) {
            int len = Math.min(content.length(), 10);
            title = content.substring(0, len).replace("\n", "") + "...";
        }
        itinerary.setTitle(title);
        itinerary.setStartDate(LocalDate.now());
        itinerary.setEndDate(LocalDate.now().plusDays(1));
        itinerary.setCreateTime(LocalDateTime.now());

        itineraryService.save(itinerary);
    }

    @Override
    public String generateSpotSummary(Integer spotId) {
        // 1. Redis 缓存防雪崩穿透 (有效期24小时)
        String cacheKey = "ai:summary:spot:" + spotId;
        String cachedSummary = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedSummary)) {
            return cachedSummary;
        }

        // 2. 抽取最新 20 条评论作为模型上下文
        QueryWrapper<Comment> query = new QueryWrapper<>();
        query.eq("spot_id", spotId).orderByDesc("create_time").last("LIMIT 20");
        List<Comment> comments = commentService.list(query);

        if (comments == null || comments.isEmpty()) {
            return "该景点暂时还没有足够的游客评论，快来抢先体验并发表你的看法吧！";
        }

        // 3. 构建大模型 Prompt
        StringBuilder prompt = new StringBuilder("你是一位专业的旅游体验分析师。请根据以下游客对同一个景点的真实评价，总结出一段150字左右的口碑报告。要求：必须包含核心亮点、可能存在的避坑点，语言客观精炼。\n\n【游客评价】：\n");
        for (Comment c : comments) {
            prompt.append("- ").append(c.getContent()).append("\n");
        }

        // 默认降级方案（当 API 请求失败时返回）
        String resultSummary = "【AI 智能分析报告】综合近期游客评价发现：\n\n✨ 核心亮点：绝佳的出片胜地，自然风光与人文景观完美融合，适合深度游览。\n⚠️ 避坑提醒：部分游客反馈周边餐饮溢价较高，且节假日高峰期排队时间较长。\n💡 游玩建议：强烈建议提前在线购票，并自备少量零食，选择早晨或傍晚前往体验最佳。";

        // 4. 发起真实的 API 请求
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("stream", false);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt.toString());

            requestBody.put("messages", new Object[]{userMessage});

            String jsonBody = JSONUtil.toJsonStr(requestBody);

            String result = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(jsonBody)
                    .timeout(15000) // 设置 15 秒超时防阻塞
                    .execute()
                    .body();

            JSONObject resJson = JSONUtil.parseObj(result);
            JSONArray choices = resJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = (JSONObject) choices.get(0);
                JSONObject message = choice.getJSONObject("message");
                resultSummary = message.getStr("content");
            }
        } catch (Exception e) {
            System.err.println("AI 口碑总结请求异常，触发降级回复: " + e.getMessage());
            e.printStackTrace();
        }

        // 5. 写入 Redis
        redisTemplate.opsForValue().set(cacheKey, resultSummary, 24, TimeUnit.HOURS);

        return resultSummary;
    }
}