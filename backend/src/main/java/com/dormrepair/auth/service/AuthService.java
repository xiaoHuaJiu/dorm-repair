package com.dormrepair.auth.service;

import com.dormrepair.auth.dto.LoginRequest;
import com.dormrepair.auth.vo.LoginResponse;
import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.SysUser;
import com.dormrepair.domain.mapper.SysUserMapper;
import com.dormrepair.security.jwt.JwtTokenService;
import com.dormrepair.security.model.LoginUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.userMapper = userMapper; this.passwordEncoder = passwordEncoder; this.tokenService = tokenService;
    }
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.LOGIN_FAILED);
        }
        if (!Integer.valueOf(1).equals(user.getStatus()) || !Integer.valueOf(0).equals(user.getDeleted())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_UNAVAILABLE);
        }
        UserRoleEnum role = UserRoleEnum.fromCode(user.getRoleType())
            .orElseThrow(() -> new BusinessException(ResultCodeEnum.ACCOUNT_UNAVAILABLE));
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRealName(), role);
        return new LoginResponse(tokenService.generate(loginUser),
            new LoginResponse.UserInfo(user.getId(), user.getUsername(), user.getRealName(), role.getCode()));
    }
}
