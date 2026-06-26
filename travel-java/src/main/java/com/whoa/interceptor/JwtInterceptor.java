package com.whoa.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whoa.utils.JwtUtils;
import com.whoa.vo.Result;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求（跨域）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("请求未携带token: {}", request.getRequestURI());
            writeError(response, 401, "未登录");
            return false;
        }

        token = token.substring(7); // 去掉 "Bearer " 前缀

        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            // 把 userId 和 username 存到 request 中，后续 Controller 可以直接取
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("token已过期: {}", e.getMessage());
            writeError(response, 401, "登录已过期，请重新登录");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("token格式错误: {}", e.getMessage());
            writeError(response, 401, "无效的token");
            return false;
        } catch (Exception e) {
            log.error("token解析异常: {}", e.getMessage());
            writeError(response, 401, "认证失败");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
    }
}
