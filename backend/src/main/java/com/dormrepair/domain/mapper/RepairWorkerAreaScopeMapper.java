package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairWorkerAreaScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairWorkerAreaScopeMapper {
    RepairWorkerAreaScope selectById(@Param("id") Long id);
    List<RepairWorkerAreaScope> selectByWorkerId(@Param("workerId") Long workerId);
    List<RepairWorkerAreaScope> selectByWorkerIdForUpdate(@Param("workerId") Long workerId);
    int insert(RepairWorkerAreaScope value);
    int updateStatus(@Param("id") Long id,@Param("status") Integer status);
}
