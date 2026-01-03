package com.example.travelaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.entity.Itinerary;
import com.example.travelaibackend.service.IItineraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/itinerary")
@CrossOrigin
public class ItineraryController {

    @Autowired
    private IItineraryService itineraryService;

    // 1. 保存行程
    @PostMapping("/add")
    public Result<?> add(@RequestBody Itinerary itinerary) {
        itinerary.setCreateTime(LocalDateTime.now());
        // 如果前端没传 userId，默认设为1 (演示用)
        if (itinerary.getUserId() == null) {
            itinerary.setUserId(1L);
        }
        itineraryService.save(itinerary);
        return Result.success("行程保存成功！");
    }

    // 2. 获取我的行程列表
    // GET /itinerary/list?userId=1
    @GetMapping("/list")
    public Result<List<Itinerary>> list(Long userId) {
        if (userId == null) userId = 1L; // 默认查 id=1
        QueryWrapper<Itinerary> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.orderByDesc("create_time");
        return Result.success(itineraryService.list(query));
    }
}