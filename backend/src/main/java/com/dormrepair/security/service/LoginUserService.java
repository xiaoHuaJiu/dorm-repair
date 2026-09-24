package com.dormrepair.security.service;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.domain.entity.SysUser;
import com.dormrepair.domain.mapper.SysUserMapper;
import com.dormrepair.security.model.LoginUser;
import org.springframework.stereotype.Service;

@Service
public class LoginUserService {
    private final SysUserMapper userMapper;
    public LoginUserService(SysUserMapper userMapper) { this.userMapper = userMapper; }
    public LoginUser refresh(LoginUser tokenUser) {
        SysUser user = userMapper.selectById(tokenUser.getUserId());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || !Integer.valueOf(0).equals(user.getDeleted())) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        UserRoleEnum role = UserRoleEnum.fromCode(user.getRoleType())
            .orElseThrow(() -> new BusinessException(ResultCodeEnum.UNAUTHORIZED));
        return new LoginUser(user.getId(), user.getUsername(), user.getRealName(), role);
    }
}
