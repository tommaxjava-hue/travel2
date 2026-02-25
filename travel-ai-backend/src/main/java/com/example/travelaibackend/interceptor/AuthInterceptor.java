package com.example.travelaibackend.interceptor;

import cn.hutool.core.util.StrUtil;
import com.example.travelaibackend.annotation.RequireSuperAdmin;
import com.example.travelaibackend.common.BusinessException;
import com.example.travelaibackend.common.ResultCode;
import com.example.travelaibackend.utils.JwtUtils;
import com.example.travelaibackend.utils.UserContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行跨域 OPTIONS 请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            // 对未携带Token的请求，由具体的方法决定是否报错，这里先尝试解析
            return checkMethodPermission(handler, null);
        }

        // 规范化处理 Bearer 格式
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long userId = JwtUtils.getUserIdFromToken(token);
        if (userId != null) {
            UserContext.setUserId(userId);
        }

        return checkMethodPermission(handler, userId);
    }

    private boolean checkMethodPermission(Object handler, Long userId) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            // 检查方法或类上是否标注了超级管理员专属注解
            RequireSuperAdmin requireSuperAdmin = handlerMethod.getMethodAnnotation(RequireSuperAdmin.class);
            if (requireSuperAdmin == null) {
                requireSuperAdmin = handlerMethod.getBeanType().getAnnotation(RequireSuperAdmin.class);
            }

            // 【强制核心规则】：如果有超级管理员注解，严格要求 ID == 88
            if (requireSuperAdmin != null) {
                if (userId == null) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED);
                }
                if (userId != 88L) {
                    throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "越权访问：该操作仅最高统帅(ID:88)可用");
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 避免内存泄漏
        UserContext.clear();
    }
}