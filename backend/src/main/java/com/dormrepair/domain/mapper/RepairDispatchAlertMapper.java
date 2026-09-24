package com.dormrepair.domain.mapper;
import com.dormrepair.domain.entity.RepairDispatchAlert;
import org.apache.ibatis.annotations.Mapper; import org.apache.ibatis.annotations.Param;
@Mapper public interface RepairDispatchAlertMapper {
 RepairDispatchAlert selectById(@Param("id") Long id);
 int upsertOpenAlert(RepairDispatchAlert alert);
}
