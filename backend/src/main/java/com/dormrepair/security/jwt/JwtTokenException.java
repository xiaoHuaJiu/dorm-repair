package com.dormrepair.security.jwt;

public class JwtTokenException extends RuntimeException {
    public JwtTokenException(Throwable cause) { super("令牌无效", cause); }
}
