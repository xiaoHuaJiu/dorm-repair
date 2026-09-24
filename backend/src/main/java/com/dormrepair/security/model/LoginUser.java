package com.dormrepair.security.model;

import com.dormrepair.common.enums.UserRoleEnum;

public final class LoginUser {
    private final Long userId;
    private final String username;
    private final String realName;
    private final UserRoleEnum roleType;

    public LoginUser(Long userId, String displayName, UserRoleEnum roleType) {
        this(userId, displayName, displayName, roleType);
    }

    public LoginUser(Long userId, String username, String realName, UserRoleEnum roleType) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roleType = roleType;
    }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRealName() { return realName; }
    public String getDisplayName() { return realName; }
    public UserRoleEnum getRoleType() { return roleType; }
}
