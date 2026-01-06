package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.service.IAttractionService;
import com.example.travelaibackend.service.IPostService;
import com.example.travelaibackend.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> map = new HashMap<>();
        map.put("userCount", userService.count());
        map.put("postCount", postService.count());
        map.put("spotCount", attractionService.count());
        // 模拟日活，实际应查Redis登录记录
        map.put("dau", userService.count() / 2 + 5);
        return Result.success(map);
    }
}