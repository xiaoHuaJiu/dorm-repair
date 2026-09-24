package com.dormrepair.security.context;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.security.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;

class UserContextTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void readsCurrentAuthenticationEveryTime() {
        authenticate(new LoginUser(1L, "学生", UserRoleEnum.STUDENT));
        assertThat(UserContext.getCurrentUserId()).isEqualTo(1L);
        assertThat(UserContext.isStudent()).isTrue();
        assertThat(UserContext.isWorker()).isFalse();
        authenticate(new LoginUser(2L, "管理员", UserRoleEnum.ADMIN));
        assertThat(UserContext.getCurrentUserId()).isEqualTo(2L);
        assertThat(UserContext.isAdmin()).isTrue();
    }

    @Test void recognizesWorker() {
        authenticate(new LoginUser(3L, "维修人员", UserRoleEnum.WORKER));
        assertThat(UserContext.isWorker()).isTrue();
        assertThat(UserContext.isStudent()).isFalse();
    }

    @Test void rejectsMissingUnauthenticatedAndUnexpectedPrincipals() {
        assertUnauthorized();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new LoginUser(1L, "学生", UserRoleEnum.STUDENT), null));
        assertUnauthorized();
        authenticate("anonymousUser");
        assertUnauthorized();
        authenticate(42L);
        assertUnauthorized();
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of()));
    }

    private void assertUnauthorized() {
        assertThatThrownBy(UserContext::getCurrentUser)
            .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getCode()).isEqualTo(10002));
    }
}
