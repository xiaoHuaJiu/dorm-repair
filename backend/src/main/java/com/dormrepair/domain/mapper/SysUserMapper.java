package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper {
    SysUser selectById(@Param("id") Long id);
    SysUser selectByUsername(@Param("username") String username);
    int insert(SysUser user);
    int updateStatus(@Param("id") Long id,@Param("status") Integer status);
}
