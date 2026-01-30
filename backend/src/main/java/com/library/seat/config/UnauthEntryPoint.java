package com.library.seat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.seat.common.Result;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 认证失败入口点，返回 401 状态码而非默认的 403
 */
@Component
public class UnauthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) 
            throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Result<String> result = Result.error(401, "身份验证已过期或无效，请重新登录");
        
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(result));
    }
}
