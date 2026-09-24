package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SysUserMapper {
    SysUser selectById(@Param("id") Long id);
    SysUser selectByUsername(@Param("username") String username);
    int insert(SysUser user);
    int updateStatus(@Param("id") Long id,@Param("status") Integer status);
    List<Long> selectEnabledAdminIds();
}
