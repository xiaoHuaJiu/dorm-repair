package com.dormrepair.domain.mapper;
import com.dormrepair.domain.entity.RepairDispatchAlert;
import org.apache.ibatis.annotations.Mapper; import org.apache.ibatis.annotations.Param;
@Mapper public interface RepairDispatchAlertMapper {
 RepairDispatchAlert selectById(@Param("id") Long id);
 int upsertOpenAlert(RepairDispatchAlert alert);
 /** 插入一条无工单维度的汇总提醒（order_id 为空），用于请假部分转派失败等汇总场景。 */
 int insertSummaryAlert(RepairDispatchAlert alert);
}
