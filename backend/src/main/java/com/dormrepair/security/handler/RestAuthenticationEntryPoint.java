package com.dormrepair.security.handler;

import com.dormrepair.common.enums.ResultCodeEnum;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityErrorResponseWriter writer;
    public RestAuthenticationEntryPoint(SecurityErrorResponseWriter writer) { this.writer = writer; }
    @Override public void commence(HttpServletRequest request, HttpServletResponse response,
                                   AuthenticationException authException) throws IOException, ServletException {
        writer.write(response, HttpServletResponse.SC_UNAUTHORIZED, ResultCodeEnum.UNAUTHORIZED);
    }
}
