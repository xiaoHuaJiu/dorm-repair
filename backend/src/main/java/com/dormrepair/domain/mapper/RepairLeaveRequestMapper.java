package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairLeaveRequest;
import com.dormrepair.leave.vo.LeaveRequestDetailResponse;
import com.dormrepair.leave.vo.LeaveRequestListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RepairLeaveRequestMapper {
    RepairLeaveRequest selectById(@Param("id") Long id);
    RepairLeaveRequest selectByIdForUpdate(@Param("id") Long id);
    int insert(RepairLeaveRequest value);
    /**
     * 同一维修人员待审批/已通过请假与新区间重叠的数量。时间区间语义为 [startTime, endTime)。
     */
    int countOverlap(@Param("workerId") Long workerId, @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime, @Param("statuses") List<Integer> statuses);
    List<LeaveRequestListItem> selectWorkerPage(@Param("workerId") Long workerId, @Param("approvalStatus") Integer approvalStatus);
    List<LeaveRequestListItem> selectAdminPage(@Param("workerId") Long workerId, @Param("approvalStatus") Integer approvalStatus,
        @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    LeaveRequestDetailResponse selectDetailByWorker(@Param("id") Long id, @Param("workerId") Long workerId);
    LeaveRequestDetailResponse selectDetailById(@Param("id") Long id);
    /** 审批 CAS：仅待审批状态可变更，防止并发重复审批。 */
    int casReview(@Param("id") Long id, @Param("expectedStatus") Integer expectedStatus, @Param("approvalStatus") Integer approvalStatus,
        @Param("reviewAdminId") Long reviewAdminId, @Param("reviewRemark") String reviewRemark, @Param("reviewTime") LocalDateTime reviewTime);
    /**
     * 生效任务扫描：已通过、已到开始时间、尚未结束，且未处理或处理中超时的请假。
     */
    List<Long> selectEffectiveIds(@Param("now") LocalDateTime now, @Param("staleBefore") LocalDateTime staleBefore, @Param("limit") int limit);
    /** CAS 抢任务：0→1，或处理中超时（异常中断）时允许 1→1 恢复执行。 */
    int casTakeForEffective(@Param("id") Long id, @Param("staleBefore") LocalDateTime staleBefore);
    /** 转派终态更新：处理中 → 已完成/部分失败。 */
    int casUpdateReassignStatus(@Param("id") Long id, @Param("expectedStatus") Integer expectedStatus, @Param("reassignStatus") Integer reassignStatus);
    /** 结束任务扫描：存在已到结束时间已通过请假的维修人员。 */
    List<Long> selectWorkerIdsWithEndedLeave(@Param("now") LocalDateTime now, @Param("limit") int limit);
    /** 当前生效请假数量（连续请假判断）。 */
    int countActiveLeave(@Param("workerId") Long workerId, @Param("now") LocalDateTime now);
}
