package com.example.travelaibackend.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.annotation.RequireSuperAdmin;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.common.ResultCode;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IUserService;
import com.example.travelaibackend.utils.JwtUtils;
import com.example.travelaibackend.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String verifyCode = (String) params.get("verifyCode");
        String verifyKey = (String) params.get("verifyKey");

        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "账号或密码不能为空");
        }

        if (StrUtil.isBlank(verifyCode) || StrUtil.isBlank(verifyKey)) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "请输入验证码");
        }
        String cachedCode = CaptchaController.CAPTCHA_STORE.get(verifyKey);
        if (cachedCode == null || !cachedCode.equalsIgnoreCase(verifyCode)) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "验证码错误或已过期");
        }
        CaptchaController.CAPTCHA_STORE.remove(verifyKey);

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", username);
        SysUser dbUser = userService.getOne(query);

        if (dbUser == null) return Result.error("401", "用户不存在");
        if ("BANNED".equals(dbUser.getPassword())) return Result.error("403", "该账号已被封禁");

        String dbPwd = dbUser.getPassword();
        boolean isMatch = false;

        if (dbPwd.startsWith("$2a$")) {
            isMatch = BCrypt.checkpw(password, dbPwd);
        } else if (dbPwd.length() == 32) {
            isMatch = dbPwd.equalsIgnoreCase(SecureUtil.md5(password));
        } else {
            isMatch = dbPwd.equals(password);
        }

        if (!isMatch) {
            return Result.error("401", "密码错误");
        }

        String token = JwtUtils.generateToken(dbUser.getUserId().longValue());
        dbUser.setPassword(null);

        try {
            String todayKey = "dau:" + LocalDate.now().toString();
            redisTemplate.opsForSet().add(todayKey, String.valueOf(dbUser.getUserId()));
            redisTemplate.expire(todayKey, 7, TimeUnit.DAYS); // 延长至7天以供图表统计
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", dbUser);
        return Result.success(result);
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String tags = (String) params.get("tags");
        String verifyCode = (String) params.get("verifyCode");
        String verifyKey = (String) params.get("verifyKey");

        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "请输入用户名和密码");
        }

        if (StrUtil.isBlank(verifyCode) || StrUtil.isBlank(verifyKey)) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "请输入验证码");
        }
        String cachedCode = CaptchaController.CAPTCHA_STORE.get(verifyKey);
        if (cachedCode == null || !cachedCode.equalsIgnoreCase(verifyCode)) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "验证码错误或已过期");
        }
        CaptchaController.CAPTCHA_STORE.remove(verifyKey);

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", username);
        if (userService.count(query) > 0) {
            return Result.error("400", "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setRole("user");
        if (StrUtil.isNotBlank(tags)) {
            user.setTags(tags);
        }
        userService.save(user);

        return Result.success("注册成功");
    }

    @RequireSuperAdmin
    @GetMapping("/list")
    public Result<List<SysUser>> list() {
        // 【核心底线】：硬性约束超级管理员 ID=88，防止拦截器被绕过
        if (UserContext.getUserId() == null || UserContext.getUserId() != 88L) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：仅最高统帅(ID:88)可查看或操作全体用户池");
        }
        return Result.success(userService.list());
    }

    @RequireSuperAdmin
    @PostMapping("/toggleStatus")
    public Result<?> toggleStatus(@RequestBody SysUser user) {
        if (UserContext.getUserId() == null || UserContext.getUserId() != 88L) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：封禁/解封功能仅最高统帅(ID:88)可用");
        }

        SysUser dbUser = userService.getById(user.getUserId());
        if (dbUser == null) return Result.error("404", "用户不存在");

        if (dbUser.getUserId() == 88) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "系统底层限制：不可更改最高权限账号状态");
        }

        if ("BANNED".equals(dbUser.getPassword())) {
            dbUser.setPassword(BCrypt.hashpw("123456"));
            userService.updateById(dbUser);
            return Result.success("已解封，密码重置为 123456");
        } else {
            dbUser.setPassword("BANNED");
            userService.updateById(dbUser);
            return Result.success("已封禁该用户");
        }
    }

    @RequireSuperAdmin
    @PostMapping("/changeRole")
    public Result<?> changeRole(@RequestBody Map<String, Object> params) {
        if (UserContext.getUserId() == null || UserContext.getUserId() != 88L) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：权限分配功能仅最高统帅(ID:88)可用");
        }

        Integer targetUserId = (Integer) params.get("targetUserId");
        String newRole = (String) params.get("newRole");

        if (targetUserId == null || StrUtil.isBlank(newRole)) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "参数错误");
        }

        if (targetUserId == 88) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "系统底层限制：不可修改最高权限账号的系统角色");
        }

        SysUser target = userService.getById(targetUserId);
        if (target == null) return Result.error("404", "用户不存在");

        target.setRole(newRole);
        userService.updateById(target);
        return Result.success("权限修改成功");
    }

    @GetMapping("/info")
    public Result<SysUser> getUserInfo() {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);

        SysUser user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("404", "用户不存在");
    }

    @PostMapping("/update")
    public Result<SysUser> updateUserInfo(@RequestBody SysUser user) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null || !currentUserId.equals(user.getUserId().longValue())) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "只能更新自己的信息");
        }

        boolean success = userService.updateById(user);
        if (success) {
            SysUser newUser = userService.getById(user.getUserId());
            newUser.setPassword(null);
            return Result.success(newUser);
        }
        return Result.error(ResultCode.FAILED.getCode(), "更新失败");
    }
}