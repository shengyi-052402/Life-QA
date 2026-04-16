package com.forum.server.interceptor;

import com.forum.common.constant.JwtConstant;
import com.forum.common.constant.MessageConstant;
import com.forum.common.context.BaseContext;
import com.forum.common.exception.BaseException;
import com.forum.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(JwtConstant.HEADER_NAME);

        // 放行逻辑：如果没有携带 Token 或者是格式不对
        if (!StringUtils.hasText(token) || !token.startsWith(JwtConstant.TOKEN_PREFIX)) {
            String uri = request.getRequestURI();
            String method = request.getMethod();
            // 允许匿名访问的公开 GET 接口
            if ("GET".equalsIgnoreCase(method) && 
               (uri.startsWith("/api/posts") || uri.startsWith("/api/categories") || uri.startsWith("/api/tags"))) {
                return true;
            }
            
            response.setStatus(401);
            return false;
        }

        // 提取 Token 并解析
        token = token.substring(JwtConstant.TOKEN_PREFIX.length());
        
        try {
            if (JwtUtil.isExpired(token)) {
                response.setStatus(401);
                return false;
            }
            Long userId = JwtUtil.getUserId(token);
            // 存入 ThreadLocal
            BaseContext.setCurrentId(userId);
            return true;
        } catch (Exception e) {
            log.error("JWT校验失败", e);
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求处理完后清理 ThreadLocal，防止内存泄漏
        BaseContext.removeCurrentId();
    }
}
