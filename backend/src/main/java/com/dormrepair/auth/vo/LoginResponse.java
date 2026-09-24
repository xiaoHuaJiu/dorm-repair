package com.dormrepair.auth.vo;

public record LoginResponse(String token, UserInfo user) {
    public record UserInfo(Long userId, String username, String realName, Integer roleType) {}
}
