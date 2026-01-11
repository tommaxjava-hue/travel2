package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.service.IAttractionService;
import com.example.travelaibackend.service.IPostService;
import com.example.travelaibackend.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate; // 1. 引入 Redis 模板
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate; // 2. 引入日期处理
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin
public class AdminController {

    @Autowired
    private IUserService userService;
    @Autowired
    private IPostService postService;
    @Autowired
    private IAttractionService attractionService;

    // 3. 注入 Redis 操作模板
    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> map = new HashMap<>();

        // 基础数据：依然查数据库
        map.put("userCount", userService.count());
        map.put("postCount", postService.count());
        map.put("spotCount", attractionService.count());

        // 4. 活跃用户：查 Redis
        // 逻辑：构建今日的 Key，例如 "dau:2023-11-15"
        String todayKey = "dau:" + LocalDate.now().toString();

        // 获取 Redis Set 集合的大小 (Set 自动去重，适合统计 DAU)
        Long dau = redisTemplate.opsForSet().size(todayKey);

        // 处理空值情况
        map.put("dau", dau != null ? dau : 0);

        return Result.success(map);
    }
}