package com.example.travelaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.annotation.RequireAdmin;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.common.ResultCode;
import com.example.travelaibackend.entity.Banner;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IBannerService;
import com.example.travelaibackend.service.IUserService;
import com.example.travelaibackend.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/banner")
@CrossOrigin
public class BannerController {

    @Autowired
    private IBannerService bannerService;

    @Autowired
    private IUserService userService;

    // 前台获取显示的轮播图（无需权限）
    @GetMapping("/list/active")
    public Result<List<Banner>> getActiveBanners() {
        QueryWrapper<Banner> query = new QueryWrapper<>();
        query.eq("is_show", 1).orderByAsc("sort_order");
        return Result.success(bannerService.list(query));
    }

    // 后台获取所有轮播图
    @RequireAdmin
    @GetMapping("/list")
    public Result<List<Banner>> list() {
        QueryWrapper<Banner> query = new QueryWrapper<>();
        query.orderByAsc("sort_order");
        return Result.success(bannerService.list(query));
    }

    @RequireAdmin
    @PostMapping("/add")
    public Result<?> add(@RequestBody Banner banner) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        boolean isAdmin = user != null && "admin".equals(user.getRole());

        // 【强制业务约束】：防线兜底，仅最高统帅(88)及管理员可用
        if (userId == null || (userId != 88L && !isAdmin)) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：仅管理员及最高统帅(ID:88)可用");
        }

        banner.setCreateTime(LocalDateTime.now());
        if (banner.getIsShow() == null) banner.setIsShow(1);
        if (banner.getSortOrder() == null) banner.setSortOrder(0);
        bannerService.save(banner);
        return Result.success("轮播图添加成功");
    }

    @RequireAdmin
    @PostMapping("/update")
    public Result<?> update(@RequestBody Banner banner) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        boolean isAdmin = user != null && "admin".equals(user.getRole());

        if (userId == null || (userId != 88L && !isAdmin)) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：仅管理员及最高统帅(ID:88)可用");
        }

        bannerService.updateById(banner);
        return Result.success("更新成功");
    }

    @RequireAdmin
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        boolean isAdmin = user != null && "admin".equals(user.getRole());

        if (userId == null || (userId != 88L && !isAdmin)) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：仅管理员及最高统帅(ID:88)可用");
        }

        bannerService.removeById(id);
        return Result.success("删除成功");
    }
}