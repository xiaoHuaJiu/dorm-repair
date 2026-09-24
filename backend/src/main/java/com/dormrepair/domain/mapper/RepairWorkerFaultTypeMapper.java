package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairWorkerFaultType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairWorkerFaultTypeMapper {
    RepairWorkerFaultType selectById(@Param("id") Long id);
    List<RepairWorkerFaultType> selectByWorkerId(@Param("workerId") Long workerId);
    List<RepairWorkerFaultType> selectByWorkerIdForUpdate(@Param("workerId") Long workerId);
    int insert(RepairWorkerFaultType value);
    int updateStatus(@Param("id") Long id,@Param("status") Integer status);
}
