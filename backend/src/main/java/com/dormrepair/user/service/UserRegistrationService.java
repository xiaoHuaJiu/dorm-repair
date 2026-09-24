package com.dormrepair.user.service;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.SysUser;
import com.dormrepair.domain.mapper.SysUserMapper;
import com.dormrepair.user.dto.StudentRegisterRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(StudentRegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_MISMATCH);
        }
        if (userMapper.selectByUsername(request.username()) != null) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName().trim());
        user.setPhone(normalizePhone(request.phone()));
        user.setRoleType(UserRoleEnum.STUDENT.getCode());
        user.setStatus(1);
        user.setDeleted(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}
