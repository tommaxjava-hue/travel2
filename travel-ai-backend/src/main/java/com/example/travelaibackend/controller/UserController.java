package com.example.travelaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private IUserService userService;

    // ================= 原有功能区域 =================

    // 1. 用户登录
    @PostMapping("/login")
    public Result<SysUser> login(@RequestBody SysUser user) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        SysUser dbUser = userService.getOne(query);

        if (dbUser == null) return Result.error("401", "用户不存在");
        if ("BANNED".equals(dbUser.getPassword())) return Result.error("403", "该账号已被封禁");
        if (!dbUser.getPassword().equals(user.getPassword())) return Result.error("401", "密码错误");

        return Result.success(dbUser);
    }

    // 2. 用户注册
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, Object> params) {
        // 如果你有验证码逻辑，请在此处保留。这里提供基础注册逻辑。
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        // 处理注册时的标签 (如果前端传了tags)
        String tags = (String) params.get("tags");

        if (username == null || password == null) return Result.error("400", "请输入用户名和密码");

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", username);
        if (userService.count(query) > 0) {
            return Result.error("400", "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
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

    // 5. 超级管理员修改用户角色 (解决权限修改)
    @PostMapping("/changeRole")
    public Result<?> changeRole(@RequestBody Map<String, Object> params) {
        Integer operatorId = (Integer) params.get("operatorId"); // 操作者ID
        Integer targetUserId = (Integer) params.get("targetUserId"); // 目标用户ID
        String newRole = (String) params.get("newRole"); // "admin" 或 "user"

        if (operatorId == null || targetUserId == null) return Result.error("400", "参数错误");

        // 鉴权：只有 ID=1 是超级管理员
        if (operatorId != 1) {
            return Result.error("403", "无权操作，只有超级管理员(ID=1)可变更权限");
        }

        if (targetUserId == 1) {
            return Result.error("403", "不能修改超级管理员的权限");
        }

        SysUser target = userService.getById(targetUserId);
        if (target == null) return Result.error("404", "用户不存在");

        target.setRole(newRole);
        userService.updateById(target);
        return Result.success("权限修改成功");
    }

    // ================= 新增功能区域 (个人中心) =================

    // 6. 获取最新用户信息 (刷新页面用)
    @GetMapping("/info")
    public Result<SysUser> getUserInfo(@RequestParam Long userId) {
        SysUser user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null); //不仅是个好习惯，更是安全保障
            return Result.success(user);
        }
        return Result.error("404", "用户不存在");
    }

    // 7. 更新用户信息 (头像、昵称、密码、标签等)
    @PostMapping("/update")
    public Result<SysUser> updateUserInfo(@RequestBody SysUser user) {
        if (user.getUserId() == null) {
            return Result.error("400", "用户ID不能为空");
        }

        // 简单校验
        boolean success = userService.updateById(user);
        if (success) {
            // 返回更新后的最新数据
            SysUser newUser = userService.getById(user.getUserId());
            newUser.setPassword(null);
            return Result.success(newUser);
        }
        return Result.error("500", "更新失败");
    }
}