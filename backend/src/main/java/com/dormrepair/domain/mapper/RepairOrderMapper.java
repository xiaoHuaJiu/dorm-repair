package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.dormrepair.order.model.RepairOrderQueryCondition;
import com.dormrepair.order.vo.RepairOrderListItem;
import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface RepairOrderMapper {
    RepairOrder selectById(@Param("id") Long id);
    RepairOrder selectByIdForUpdate(@Param("id") Long id);
    int insert(RepairOrder order);
    List<RepairOrderListItem> selectPage(RepairOrderQueryCondition condition);
    List<com.dormrepair.order.vo.SuspectedRepairOrder> selectSuspectedDuplicateOrders(
        @Param("campusId") Long campusId,@Param("areaId") Long areaId,@Param("buildingId") Long buildingId,
        @Param("roomId") Long roomId,@Param("faultTypeId") Long faultTypeId,@Param("sinceTime") LocalDateTime sinceTime,
        @Param("openStatuses") List<Integer> openStatuses);
    int casAssign(@Param("id") Long id,@Param("expectedStatus") Integer expectedStatus,@Param("expectedAssigneeId") Long expectedAssigneeId,
        @Param("workerId") Long workerId,@Param("dispatchTime") LocalDateTime dispatchTime,@Param("acceptDeadline") LocalDateTime acceptDeadline);
    int casMarkPending(@Param("id") Long id,@Param("expectedStatus") Integer expectedStatus,@Param("expectedAssigneeId") Long expectedAssigneeId);
    List<Long> selectPendingDispatchIds(@Param("before") LocalDateTime before,@Param("limit") int limit);
    int casAccept(@Param("id") Long id,@Param("workerId") Long workerId,@Param("acceptTime") LocalDateTime acceptTime,
        @Param("expectedCompleteTime") LocalDateTime expectedCompleteTime,@Param("completeDeadline") LocalDateTime completeDeadline);
    int casInterrupt(@Param("id") Long id,@Param("workerId") Long workerId);
    int casResume(@Param("id") Long id,@Param("workerId") Long workerId);
    int casSubmitResult(@Param("id") Long id,@Param("workerId") Long workerId,@Param("repairSubmitTime") LocalDateTime repairSubmitTime);
}
