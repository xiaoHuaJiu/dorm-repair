package com.dormrepair.security;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.security.context.UserContext;
import com.dormrepair.security.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DataScopePatternTest {
    record OwnedResource(Long ownerUserId, Long assigneeUserId) {}

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void studentAndWorkerCanOnlyAccessOwnedResourcesWhileAdminCanAccessAll() {
        OwnedResource resource = new OwnedResource(10L, 20L);
        authenticate(10L, UserRoleEnum.STUDENT); assertThatCode(() -> requireAccess(resource)).doesNotThrowAnyException();
        authenticate(11L, UserRoleEnum.STUDENT); assertForbidden(resource);
        authenticate(20L, UserRoleEnum.WORKER); assertThatCode(() -> requireAccess(resource)).doesNotThrowAnyException();
        authenticate(21L, UserRoleEnum.WORKER); assertForbidden(resource);
        authenticate(99L, UserRoleEnum.ADMIN); assertThatCode(() -> requireAccess(resource)).doesNotThrowAnyException();
    }

    private void requireAccess(OwnedResource resource) {
        LoginUser user = UserContext.getCurrentUser();
        boolean allowed = user.getRoleType() == UserRoleEnum.ADMIN
            || user.getRoleType() == UserRoleEnum.STUDENT && user.getUserId().equals(resource.ownerUserId())
            || user.getRoleType() == UserRoleEnum.WORKER && user.getUserId().equals(resource.assigneeUserId());
        if (!allowed) throw new BusinessException(com.dormrepair.common.enums.ResultCodeEnum.FORBIDDEN);
    }
    private void authenticate(Long id, UserRoleEnum role) {
        LoginUser user = new LoginUser(id, "test", "测试", role);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }
    private void assertForbidden(OwnedResource resource) {
        assertThatThrownBy(() -> requireAccess(resource)).isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo(10003));
    }
}
