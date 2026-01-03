package com.example.travelaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.entity.Attraction;
import com.example.travelaibackend.entity.UserFavorite;
import com.example.travelaibackend.mapper.UserFavoriteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/favorite")
@CrossOrigin
public class FavoriteController {

    @Autowired(required = false)
    private UserFavoriteMapper favoriteMapper;

    // 1. 切换收藏状态 (点赞/取消)
    @PostMapping("/toggle")
    public Result<?> toggle(@RequestBody UserFavorite fav) {
        if (fav.getUserId() == null) return Result.error("401", "请先登录");

        QueryWrapper<UserFavorite> query = new QueryWrapper<>();
        query.eq("user_id", fav.getUserId()).eq("spot_id", fav.getSpotId());

        if (favoriteMapper.selectCount(query) > 0) {
            favoriteMapper.delete(query);
            return Result.success("已取消收藏");
        } else {
            fav.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(fav);
            return Result.success("收藏成功");
        }
    }

    // 2. 检查某个景点是否已收藏 (用于详情页亮灯)
    @GetMapping("/check")
    public Result<Boolean> check(Long userId, Long spotId) {
        QueryWrapper<UserFavorite> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("spot_id", spotId);
        return Result.success(favoriteMapper.selectCount(query) > 0);
    }

    // 3. 【新增】获取我的收藏列表
    @GetMapping("/list")
    public Result<List<Attraction>> getMyFavorites(Long userId) {
        return Result.success(favoriteMapper.selectUserFavoriteSpots(userId));
    }
}