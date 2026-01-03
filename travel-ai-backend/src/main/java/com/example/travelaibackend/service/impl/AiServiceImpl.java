package com.example.travelaibackend.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.entity.Attraction;
import com.example.travelaibackend.service.IAiService;
import com.example.travelaibackend.service.IAttractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements IAiService {

    @Value("${ai.url}")
    private String apiUrl;

    @Value("${ai.key}")
    private String apiKey;

    @Value("${ai.model}")
    private String modelName;

    @Autowired
    private IAttractionService attractionService;

    @Override
    public String chat(String userQuestion) {
        // 1. 【RAG 检索增强】简单的关键词搜索
        // 如果用户问了“上海”，我们就先把数据库里上海的景点查出来告诉 AI
        // 这里的逻辑可以写得很复杂，为了演示，我们只做简单的模糊匹配
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
        requestBody.put("stream", false); // 暂时不使用流式，简单点

        // 消息列表
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
            System.out.println("正在请求AI接口: " + jsonBody); // 打印日志方便调试

            String result = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(jsonBody)
                    .execute()
                    .body();

            System.out.println("AI返回原始结果: " + result);

            // 5. 解析返回结果
            JSONObject jsonObject = JSONUtil.parseObj(result);
            // 提取 content 字段：choices[0].message.content
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
}