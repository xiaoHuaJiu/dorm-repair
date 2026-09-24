package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairFaultType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairFaultTypeMapper {
    RepairFaultType selectById(@Param("id") Long id);
    int insert(RepairFaultType value);
    int update(RepairFaultType value);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    List<RepairFaultType> selectPage(@Param("typeCode") String typeCode, @Param("typeName") String typeName,
                                     @Param("status") Integer status);
    List<RepairFaultType> selectEnabled();
}
