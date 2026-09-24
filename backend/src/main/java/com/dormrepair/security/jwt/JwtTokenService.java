package com.dormrepair.security.jwt;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.security.model.LoginUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final JwtProperties properties;
    private final SecretKey key;
    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        if (properties.getSecret() == null || properties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT 密钥长度不能少于 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
    public String generate(LoginUser user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getUserId().toString())
            .claim("username", user.getUsername()).claim("realName", user.getRealName())
            .claim("roleType", user.getRoleType().getCode()).issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.getTtl()))).signWith(key).compact();
    }
    public LoginUser parse(String token) {
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            Integer roleCode = claims.get("roleType", Integer.class);
            UserRoleEnum role = UserRoleEnum.fromCode(roleCode).orElseThrow();
            return new LoginUser(userId, claims.get("username", String.class), claims.get("realName", String.class), role);
        } catch (Exception exception) {
            throw new JwtTokenException(exception);
        }
    }
}
