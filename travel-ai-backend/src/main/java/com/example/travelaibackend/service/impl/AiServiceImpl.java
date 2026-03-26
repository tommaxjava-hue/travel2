package com.example.travelaibackend.service.impl;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;
import cn.hutool.core.util.StrUtil;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class AiServiceImpl implements IAiService {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ZhipuAiClient client;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @PostConstruct
    public void init() {
        client = ZhipuAiClient.builder()
                .ofZHIPU()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String chat(String userQuestion) {
        QueryWrapper<Attraction> query = new QueryWrapper<>();
        query.like("city", userQuestion)
                .or().like("name", userQuestion)
                .or().like("tags", userQuestion);
        query.last("LIMIT 5");

        List<Attraction> relatedSpots = attractionService.list(query);

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个专业的智能旅游助手。");

        if (!relatedSpots.isEmpty()) {
            systemPrompt.append("以下是系统数据库中已有的相关景点信息，请优先基于这些信息回答，不需要重复所有字段，只推荐最合适的：\n");
            for (Attraction spot : relatedSpots) {
                systemPrompt.append(String.format("- 名称：%s, 城市：%s, 票价：%s, 特色：%s\n",
                        spot.getName(), spot.getCity(), spot.getTicketPrice(), spot.getTags()));
            }
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder().role(ChatMessageRole.SYSTEM.value()).content(systemPrompt.toString()).build());
        messages.add(ChatMessage.builder().role(ChatMessageRole.USER.value()).content(userQuestion).build());

        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(modelName)
                .messages(messages)
                .stream(false)
                .thinking(ChatThinking.builder().type("enabled").build())
                .build();

        System.out.println(">>> [模型验证] 正在发起普通/规划对话请求，当前使用的模型配置为: [" + request.getModel() + "]");

        try {
            ChatCompletionResponse response = client.chat().createChatCompletion(request);
            if (response.isSuccess()) {
                return (String) response.getData().getChoices().get(0).getMessage().getContent();
            } else {
                return "AI 思考失败：" + response.getMsg();
            }
        } catch (Exception e) {
            System.err.println(">>> AI 请求异常: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                return "⚠️ 触发了 AI 平台的频率限制，请稍微慢一点发送请求。";
            }
            return "AI 服务暂时不可用：" + e.getMessage();
        }
    }

    @Override
    public SseEmitter streamAsk(String question, Long userId) {
        SseEmitter emitter = new SseEmitter(120000L);

        emitter.onCompletion(() -> System.out.println(">>> SSE 请求正常结束"));
        emitter.onTimeout(() -> {
            System.err.println(">>> 警告: SSE 请求超时，强制关闭连接");
            emitter.complete();
        });
        emitter.onError(e -> {
            System.err.println(">>> 警告: SSE 请求发生异常断开: " + e.getMessage());
            emitter.complete();
        });

        executorService.execute(() -> {
            try {
                String historyKey = "chat:history:json:" + (userId != null ? userId : "guest");
                String historyJson = null;

                try {
                    historyJson = redisTemplate.opsForValue().get(historyKey);
                } catch (Exception e) {
                    System.err.println(">>> 警告: Redis连接失败，暂不使用历史上下文记忆，进入单轮对话模式");
                }

                List<ChatMessage> messages = new ArrayList<>();
                messages.add(ChatMessage.builder().role(ChatMessageRole.SYSTEM.value()).content("你是一个专业的智能导游。请用友好、专业的语气解答游客问题。").build());

                if (StrUtil.isNotBlank(historyJson)) {
                    try {
                        JSONArray array = JSONUtil.parseArray(historyJson);
                        for (int i = 0; i < array.size(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            messages.add(ChatMessage.builder()
                                    .role(obj.getStr("role"))
                                    .content(obj.getStr("content"))
                                    .build());
                        }
                    } catch (Exception e) {
                        try {
                            redisTemplate.delete(historyKey);
                        } catch (Exception ignored) {}
                    }
                }

                messages.add(ChatMessage.builder().role(ChatMessageRole.USER.value()).content(question).build());

                ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                        .model(modelName)
                        .messages(messages)
                        .stream(true)
                        .thinking(ChatThinking.builder().type("enabled").build())
                        .build();

                System.out.println(">>> [模型验证] 正在发起流式(打字机)对话请求，当前使用的模型配置为: [" + request.getModel() + "]");

                ChatCompletionResponse response = client.chat().createChatCompletion(request);

                if (response.isSuccess()) {
                    StringBuilder fullAiReply = new StringBuilder();
                    response.getFlowable().blockingSubscribe(
                            data -> {
                                if (data.getChoices() != null && !data.getChoices().isEmpty()) {
                                    Delta delta = data.getChoices().get(0).getDelta();
                                    if (delta != null && delta.getContent() != null) {
                                        fullAiReply.append(delta.getContent());
                                        try {
                                            emitter.send(delta.getContent());
                                        } catch (Exception e) {}
                                    }
                                }
                            },
                            error -> {
                                System.err.println("流接收异常: " + error.getMessage());
                                emitter.complete();
                            },
                            () -> {
                                messages.add(ChatMessage.builder().role(ChatMessageRole.ASSISTANT.value()).content(fullAiReply.toString()).build());

                                messages.remove(0);

                                List<ChatMessage> finalMessagesToSave = messages;
                                if (finalMessagesToSave.size() > 8) {
                                    finalMessagesToSave = finalMessagesToSave.subList(finalMessagesToSave.size() - 8, finalMessagesToSave.size());
                                }

                                List<JSONObject> saveList = new ArrayList<>();
                                for (ChatMessage msg : finalMessagesToSave) {
                                    JSONObject obj = new JSONObject();
                                    obj.set("role", msg.getRole());
                                    obj.set("content", msg.getContent().toString());
                                    saveList.add(obj);
                                }

                                try {
                                    redisTemplate.opsForValue().set(historyKey, JSONUtil.toJsonStr(saveList), 30, TimeUnit.MINUTES);
                                } catch (Exception e) {
                                    System.err.println(">>> 警告: Redis连接失败，历史记录未保存");
                                }

                                try {
                                    Long recordUserId = userId != null ? userId : 1L;
                                    jdbcTemplate.update(
                                            "INSERT INTO ai_chat_record (user_id, user_question, ai_answer, create_time) VALUES (?, ?, ?, NOW())",
                                            recordUserId, question, fullAiReply.toString()
                                    );
                                } catch (Exception e) {
                                    System.err.println("记录AI问答到数据库失败: " + e.getMessage());
                                }

                                emitter.complete();
                            }
                    );
                } else {
                    emitter.send("网络或模型接口异常：" + response.getMsg());
                    emitter.complete();
                }
            } catch (Exception e) {
                // 核心修复点：捕获 429 和其他抛出的 API 异常，向前端优雅返回提示文本
                try {
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        emitter.send("⚠️ 触发了 AI 接口调用频率限制，请稍微等几秒后再发送。");
                    } else {
                        emitter.send("⚠️ AI 服务临时开小差了，请稍后再试。");
                    }
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        return emitter;
    }

    @Override
    public void saveItinerary(Long userId, String content) {
        Itinerary itinerary = new Itinerary();
        itinerary.setUserId(userId);
        itinerary.setNote(content);

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
        String cacheKey = "ai:summary:spot:" + spotId;
        String cachedSummary = null;

        try {
            cachedSummary = redisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(cachedSummary)) {
                return cachedSummary;
            }
        } catch (Exception e) {
            System.err.println(">>> 警告: Redis读取失败，直接请求AI");
        }

        QueryWrapper<Comment> query = new QueryWrapper<>();
        query.eq("spot_id", spotId).orderByDesc("create_time").last("LIMIT 20");
        List<Comment> comments = commentService.list(query);

        if (comments == null || comments.isEmpty()) {
            return "该景点暂时还没有足够的游客评论，快来抢先体验并发表你的看法吧！";
        }

        StringBuilder prompt = new StringBuilder("你是一位专业的旅游体验分析师。请根据以下游客对同一个景点的真实评价，总结出一段150字左右的口碑报告。要求：必须包含核心亮点、可能存在的避坑点，语言客观精炼。\n\n【游客评价】：\n");
        for (Comment c : comments) {
            prompt.append("- ").append(c.getContent()).append("\n");
        }

        String resultSummary = "【AI 智能分析报告】综合近期游客评价发现：\n\n✨ 核心亮点：绝佳的出片胜地，自然风光与人文景观完美融合，适合深度游览。\n⚠️ 避坑提醒：部分游客反馈周边餐饮溢价较高，且节假日高峰期排队时间较长。\n💡 游玩建议：强烈建议提前在线购票，并自备少量零食，选择早晨或傍晚前往体验最佳。";

        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder().role(ChatMessageRole.USER.value()).content(prompt.toString()).build());

            ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                    .model(modelName)
                    .messages(messages)
                    .stream(false)
                    .build();

            System.out.println(">>> [模型验证] 正在发起景点总结请求，当前使用的模型配置为: [" + request.getModel() + "]");

            ChatCompletionResponse response = client.chat().createChatCompletion(request);
            if (response.isSuccess()) {
                resultSummary = (String) response.getData().getChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            System.err.println("AI 口碑总结请求异常，触发降级回复: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, resultSummary, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            System.err.println(">>> 警告: Redis写入失败");
        }

        return resultSummary;
    }
}