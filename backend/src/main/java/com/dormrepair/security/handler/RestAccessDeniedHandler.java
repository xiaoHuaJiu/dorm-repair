package com.dormrepair.security.handler;

import com.dormrepair.common.enums.ResultCodeEnum;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityErrorResponseWriter writer;
    public RestAccessDeniedHandler(SecurityErrorResponseWriter writer) { this.writer = writer; }
    @Override public void handle(HttpServletRequest request, HttpServletResponse response,
                                 AccessDeniedException accessDeniedException) throws IOException, ServletException {
        writer.write(response, HttpServletResponse.SC_FORBIDDEN, ResultCodeEnum.FORBIDDEN);
    }
}
