package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairTransferRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.dormrepair.transfer.dto.TransferQueryRequest;
import com.dormrepair.transfer.vo.TransferRequestView;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RepairTransferRequestMapper {
    RepairTransferRequest selectById(@Param("id") Long id);
    RepairTransferRequest selectByIdForUpdate(@Param("id") Long id);
    RepairTransferRequest selectPendingByOrderId(@Param("orderId") Long orderId);
    int insert(RepairTransferRequest request);
    int casReview(@Param("id")Long id,@Param("approvalStatus")Integer approvalStatus,@Param("adminId")Long adminId,@Param("remark")String remark,@Param("now")LocalDateTime now);
    int markExecution(@Param("id")Long id,@Param("executeStatus")Integer executeStatus,@Param("newAssigneeId")Long newAssigneeId,@Param("failureReason")String failureReason,@Param("now")LocalDateTime now);
    List<TransferRequestView> selectPage(@Param("query")TransferQueryRequest query,@Param("workerId")Long workerId);
    TransferRequestView selectViewById(@Param("id")Long id);
}
