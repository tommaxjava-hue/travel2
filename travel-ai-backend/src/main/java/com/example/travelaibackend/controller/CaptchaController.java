package com.example.travelaibackend.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.lang.UUID;
import com.example.travelaibackend.common.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/captcha")
@CrossOrigin
public class CaptchaController {

    // 简单的内存存储验证码 (Key: UUID, Value: Code)
    // 实际生产环境建议用 Redis，这里为了简化配置直接用 Map
    public static final Map<String, String> CAPTCHA_STORE = new ConcurrentHashMap<>();

    @GetMapping("/image")
    public Result<Map<String, String>> getCaptcha() {
        // 1. 生成验证码 (宽200, 高100, 4个字符, 20个干扰圈)
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(200, 100, 4, 20);

        // 2. 获取验证码内容
        String code = captcha.getCode();

        // 3. 生成唯一标识
        String key = UUID.randomUUID().toString();

        // 4. 存入内存 (后续在注册接口里验证)
        CAPTCHA_STORE.put(key, code);

        // 5. 返回 Base64 图片和 Key 给前端
        Map<String, String> map = new HashMap<>();
        map.put("key", key);
        map.put("image", captcha.getImageBase64Data());

        return Result.success(map);
    }
}