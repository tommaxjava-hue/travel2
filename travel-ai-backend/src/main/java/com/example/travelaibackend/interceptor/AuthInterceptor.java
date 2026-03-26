package com.example.travelaibackend.interceptor;

import cn.hutool.core.util.StrUtil;
import com.example.travelaibackend.annotation.RequireSuperAdmin;
import com.example.travelaibackend.annotation.RequireAdmin;
import com.example.travelaibackend.common.BusinessException;
import com.example.travelaibackend.common.ResultCode;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IUserService;
import com.example.travelaibackend.utils.JwtUtils;
import com.example.travelaibackend.utils.UserContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            return checkMethodPermission(request, handler, null);
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long userId = JwtUtils.getUserIdFromToken(token);
        if (userId != null) {
            UserContext.setUserId(userId);
        }

        return checkMethodPermission(request, handler, userId);
    }

    private boolean checkMethodPermission(HttpServletRequest request, Object handler, Long userId) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            RequireSuperAdmin requireSuperAdmin = handlerMethod.getMethodAnnotation(RequireSuperAdmin.class);
            if (requireSuperAdmin == null) {
                requireSuperAdmin = handlerMethod.getBeanType().getAnnotation(RequireSuperAdmin.class);
            }

            RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
            if (requireAdmin == null) {
                requireAdmin = handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
            }

            // 【强制核心规则】：最高统帅特权，严格要求 ID == 88
            if (requireSuperAdmin != null) {
                if (userId == null) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
                }
                if (userId != 88L) {
                    throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "越权访问：该操作仅最高统帅(ID:88)可用");
                }
            }

            // 权限下放：普通管理员权限校验
            if (requireAdmin != null) {
                if (userId == null) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
                }
                // 最高统帅(ID:88)拥有绝对特权，无视身份限制直接放行
                if (userId == 88L) {
                    return true;
                }

                IUserService userService = WebApplicationContextUtils
                        .getRequiredWebApplicationContext(request.getServletContext())
                        .getBean(IUserService.class);
                SysUser user = userService.getById(userId);

                if (user == null || !"admin".equals(user.getRole())) {
                    throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "越权访问：此操作需要管理员权限");
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}