package com.dormrepair.auth.service;

import com.dormrepair.auth.dto.LoginRequest;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.SysUser;
import com.dormrepair.domain.mapper.SysUserMapper;
import com.dormrepair.security.jwt.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final SysUserMapper mapper = mock(SysUserMapper.class);
    private final JwtTokenService tokens = mock(JwtTokenService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AuthService service = new AuthService(mapper, encoder, tokens);

    @Test void logsInEnabledUserWithoutReturningPassword() {
        SysUser user = user("alice", encoder.encode("correct"), 1, 0);
        when(mapper.selectByUsername("alice")).thenReturn(user);
        when(tokens.generate(any())).thenReturn("jwt-token");
        var response = service.login(new LoginRequest("alice", "correct"));
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().username()).isEqualTo("alice");
    }

    @Test void missingUserAndWrongPasswordExposeSameFailure() {
        when(mapper.selectByUsername("missing")).thenReturn(null);
        BusinessException missing = catchThrowableOfType(
            () -> service.login(new LoginRequest("missing", "x")), BusinessException.class);
        when(mapper.selectByUsername("alice")).thenReturn(user("alice", encoder.encode("correct"), 1, 0));
        BusinessException wrong = catchThrowableOfType(
            () -> service.login(new LoginRequest("alice", "wrong")), BusinessException.class);
        assertThat(missing.getCode()).isEqualTo(wrong.getCode());
        assertThat(missing.getMessage()).isEqualTo(wrong.getMessage());
    }

    @Test void disabledOrDeletedUsersCannotLogin() {
        when(mapper.selectByUsername("disabled")).thenReturn(user("disabled", encoder.encode("x"), 0, 0));
        assertThatThrownBy(() -> service.login(new LoginRequest("disabled", "x")))
            .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getCode()).isEqualTo(11002));
    }

    private SysUser user(String username, String password, int status, int deleted) {
        SysUser user = new SysUser(); user.setId(1L); user.setUsername(username); user.setPassword(password);
        user.setRealName("测试用户"); user.setRoleType(1); user.setStatus(status); user.setDeleted(deleted); return user;
    }
}
