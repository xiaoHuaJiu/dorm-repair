package com.dormrepair.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void writesUnifiedUnauthorizedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new RestAuthenticationEntryPoint(new SecurityErrorResponseWriter(mapper)).commence(
            new MockHttpServletRequest(), response, new InsufficientAuthenticationException("missing"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(mapper.readTree(response.getContentAsString()).get("code").asInt()).isEqualTo(10002);
    }

    @Test void writesUnifiedForbiddenResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new RestAccessDeniedHandler(new SecurityErrorResponseWriter(mapper)).handle(
            new MockHttpServletRequest(), response, new AccessDeniedException("denied"));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(mapper.readTree(response.getContentAsString()).get("code").asInt()).isEqualTo(10003);
    }
}
