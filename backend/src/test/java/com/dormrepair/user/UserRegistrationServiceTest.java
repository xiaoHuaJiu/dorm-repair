package com.dormrepair.user;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.SysUser;
import com.dormrepair.domain.mapper.SysUserMapper;
import com.dormrepair.user.dto.StudentRegisterRequest;
import com.dormrepair.user.service.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {
    @Mock SysUserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;

    @Test
    void registersOnlyStudentWithEncodedPassword() {
        when(passwordEncoder.encode("StrongPass123")).thenReturn("bcrypt-value");
        UserRegistrationService service = new UserRegistrationService(userMapper, passwordEncoder);

        service.register(new StudentRegisterRequest("student-new", "StrongPass123", "StrongPass123", "学生甲", "13800138000"));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("bcrypt-value");
        assertThat(captor.getValue().getRoleType()).isEqualTo(UserRoleEnum.STUDENT.getCode());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getDeleted()).isEqualTo(0);
    }

    @Test
    void rejectsMismatchedPasswordAndConvertsUniqueConflict() {
        UserRegistrationService service = new UserRegistrationService(userMapper, passwordEncoder);
        assertThatThrownBy(() -> service.register(new StudentRegisterRequest(
            "student-new", "StrongPass123", "Different123", "学生甲", null)))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(ResultCodeEnum.PASSWORD_MISMATCH.getCode()));

        when(passwordEncoder.encode("StrongPass123")).thenReturn("bcrypt-value");
        doThrow(new DuplicateKeyException("uk_sys_user_username")).when(userMapper).insert(any(SysUser.class));
        assertThatThrownBy(() -> service.register(new StudentRegisterRequest(
            "student-new", "StrongPass123", "StrongPass123", "学生甲", null)))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(ResultCodeEnum.USERNAME_EXISTS.getCode()));
    }
}
