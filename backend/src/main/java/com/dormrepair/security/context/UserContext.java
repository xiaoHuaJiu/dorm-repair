package com.dormrepair.security.context;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.security.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class UserContext {
    private UserContext() {}

    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long getCurrentUserId() { return getCurrentUser().getUserId(); }
    public static UserRoleEnum getCurrentRoleType() { return getCurrentUser().getRoleType(); }
    public static boolean isAdmin() { return getCurrentRoleType() == UserRoleEnum.ADMIN; }
    public static boolean isWorker() { return getCurrentRoleType() == UserRoleEnum.WORKER; }
    public static boolean isStudent() { return getCurrentRoleType() == UserRoleEnum.STUDENT; }
}
