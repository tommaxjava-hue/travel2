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
        if (itinerary.getUserId() == null) {
            itinerary.setUserId(1L);
        }
        itineraryService.save(itinerary);
        return Result.success("行程保存成功！");
    }

    // 2. 获取我的行程列表
    @GetMapping("/list")
    public Result<List<Itinerary>> list(Long userId) {
        if (userId == null) userId = 1L;
        QueryWrapper<Itinerary> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.orderByDesc("create_time");
        return Result.success(itineraryService.list(query));
    }

    // 3. 【新增功能】删除历史行程，闭合论文表2.10的业务需求
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean success = itineraryService.removeById(id);
        if (success) {
            return Result.success("行程已成功删除");
        } else {
            return Result.error("500", "行程删除失败，该记录可能已被移除");
        }
    }
}