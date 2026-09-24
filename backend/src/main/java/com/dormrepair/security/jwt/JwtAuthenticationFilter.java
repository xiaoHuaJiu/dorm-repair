package com.dormrepair.security.jwt;

import com.dormrepair.security.model.LoginUser;
import com.dormrepair.security.service.LoginUserService;
import com.dormrepair.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService tokenService;
    private final LoginUserService loginUserService;
    public JwtAuthenticationFilter(JwtTokenService tokenService, LoginUserService loginUserService) {
        this.tokenService = tokenService; this.loginUserService = loginUserService;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (!token.isBlank() && !token.contains(" ")) {
                try {
                    LoginUser user = loginUserService.refresh(tokenService.parse(token));
                    var authority = new SimpleGrantedAuthority("ROLE_" + user.getRoleType().name());
                    var authentication = UsernamePasswordAuthenticationToken.authenticated(user, token, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (JwtTokenException | BusinessException ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
