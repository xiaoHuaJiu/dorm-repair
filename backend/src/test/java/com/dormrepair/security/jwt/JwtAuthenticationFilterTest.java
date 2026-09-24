package com.dormrepair.security.jwt;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.security.model.LoginUser;
import com.dormrepair.security.service.LoginUserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtTokenService tokens = mock(JwtTokenService.class);
    private final LoginUserService users = mock(LoginUserService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, users);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void validBearerCreatesRoleAuthentication() throws Exception {
        LoginUser parsed = new LoginUser(2L, "worker", "维修员", UserRoleEnum.WORKER);
        when(tokens.parse("valid")).thenReturn(parsed); when(users.refresh(parsed)).thenReturn(parsed);
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer valid");
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
            .extracting(Object::toString).containsExactly("ROLE_WORKER");
    }

    @Test void malformedOrInvalidTokensNeverCreateAuthentication() throws Exception {
        for (String header : new String[]{"Basic x", "Bearer ", "Bearer Bearer x", "Bearer bad"}) {
            SecurityContextHolder.clearContext();
            MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", header);
            if (header.equals("Bearer bad")) when(tokens.parse("bad")).thenThrow(new JwtTokenException(new RuntimeException()));
            filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Test void unexpectedInfrastructureFailureIsNotHiddenAsUnauthorized() {
        LoginUser parsed = new LoginUser(2L, "worker", "维修员", UserRoleEnum.WORKER);
        when(tokens.parse("valid")).thenReturn(parsed);
        when(users.refresh(parsed)).thenThrow(new IllegalStateException("database unavailable"));
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer valid");
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
            filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class))))
            .isInstanceOf(IllegalStateException.class);
    }
}
