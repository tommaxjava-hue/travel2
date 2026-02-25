package com.example.travelaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate; // 🔥 1. 新增导入
import org.springframework.web.bind.annotation.*;
import cn.hutool.crypto.digest.BCrypt;

import java.time.LocalDate; // 🔥 2. 新增导入
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit; // 🔥 3. 新增导入

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private IUserService userService;

    // 🔥 4. 注入 Redis 模板
    @Autowired
    private StringRedisTemplate redisTemplate;

    // ================= 原有功能区域 =================

    // 1. 用户登录
    @PostMapping("/login")
    public Result<SysUser> login(@RequestBody SysUser user) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        SysUser dbUser = userService.getOne(query);

        if (dbUser == null) return Result.error("401", "用户不存在");
        if ("BANNED".equals(dbUser.getPassword())) return Result.error("403", "该账号已被封禁");
        if (!BCrypt.checkpw(user.getPassword(), dbUser.getPassword())) {
            return Result.error("401", "密码错误");
        }

        // 🔥 5. 【新增】记录日活 (DAU) 逻辑
        try {
            // 生成当天的 Key，例如: dau:2023-12-01
            String todayKey = "dau:" + LocalDate.now().toString();
            // 将用户ID放入 Set 集合（自动去重）
            redisTemplate.opsForSet().add(todayKey, String.valueOf(dbUser.getUserId()));
            // 设置过期时间（保留3天，避免 Redis 内存爆满）
            redisTemplate.expire(todayKey, 3, TimeUnit.DAYS);
        } catch (Exception e) {
            // 捕获 Redis 异常，防止因为 Redis 挂了导致用户无法登录
            System.err.println("Redis 写入失败: " + e.getMessage());
        }

        return Result.success(dbUser);
    }

    // 2. 用户注册
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String tags = (String) params.get("tags");

        if (username == null || password == null) return Result.error("400", "请输入用户名和密码");

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", username);
        if (userService.count(query) > 0) {
            return Result.error("400", "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        user.setPassword(hashedPassword);
        user.setRole("user"); // 默认角色
        if (tags != null) {
            user.setTags(tags);
        }
        userService.save(user);

        return Result.success("注册成功");
    }

    // 3. 管理员获取用户列表
    @GetMapping("/list")
    public Result<List<SysUser>> list() {
        return Result.success(userService.list());
    }

    // 4. 封禁/解封用户
    @PostMapping("/toggleStatus")
    public Result<?> toggleStatus(@RequestBody SysUser user) {
        SysUser dbUser = userService.getById(user.getUserId());
        if (dbUser == null) return Result.error("404", "用户不存在");
        if ("admin".equals(dbUser.getRole())) return Result.error("403", "无法封禁管理员账号");

        if ("BANNED".equals(dbUser.getPassword())) {
            dbUser.setPassword("123456"); // 解封重置密码
            userService.updateById(dbUser);
            return Result.success("已解封，密码重置为 123456");
        } else {
            dbUser.setPassword("BANNED"); // 封号
            userService.updateById(dbUser);
            return Result.success("已封禁该用户");
        }
    }

    // 5. 超级管理员修改用户角色
    @PostMapping("/changeRole")
    public Result<?> changeRole(@RequestBody Map<String, Object> params) {
        Integer operatorId = (Integer) params.get("operatorId");
        Integer targetUserId = (Integer) params.get("targetUserId");
        String newRole = (String) params.get("newRole");

        if (operatorId == null || targetUserId == null) return Result.error("400", "参数错误");

        if (operatorId != 88) {
            return Result.error("403", "无权操作，只有超级管理员(ID=1)可变更权限");
        }

        if (targetUserId == 88) {
            return Result.error("403", "不能修改超级管理员的权限");
        }

        SysUser target = userService.getById(targetUserId);
        if (target == null) return Result.error("404", "用户不存在");

        target.setRole(newRole);
        userService.updateById(target);
        return Result.success("权限修改成功");
    }

    // ================= 新增功能区域 (个人中心) =================

    // 6. 获取最新用户信息
    @GetMapping("/info")
    public Result<SysUser> getUserInfo(@RequestParam Long userId) {
        SysUser user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("404", "用户不存在");
    }

    // 7. 更新用户信息
    @PostMapping("/update")
    public Result<SysUser> updateUserInfo(@RequestBody SysUser user) {
        if (user.getUserId() == null) {
            return Result.error("400", "用户ID不能为空");
        }

        boolean success = userService.updateById(user);
        if (success) {
            SysUser newUser = userService.getById(user.getUserId());
            newUser.setPassword(null);
            return Result.success(newUser);
        }
        return Result.error("500", "更新失败");
    }
}