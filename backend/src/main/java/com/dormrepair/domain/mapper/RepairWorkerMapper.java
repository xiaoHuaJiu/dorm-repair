package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairWorker;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.dormrepair.worker.vo.WorkerDetailResponse;
import java.util.List;
import java.time.LocalDateTime;
import com.dormrepair.dispatch.model.DispatchCandidate;

@Mapper
public interface RepairWorkerMapper {
    RepairWorker selectById(@Param("id") Long id);
    RepairWorker selectByUserId(@Param("userId") Long userId);
    RepairWorker selectByWorkerNo(@Param("workerNo") String workerNo);
    int insert(RepairWorker value);
    int updateStatus(@Param("id") Long id,@Param("workStatus") Integer workStatus);
    int casUpdateStatus(@Param("id") Long id,@Param("expectedStatus") Integer expectedStatus,@Param("workStatus") Integer workStatus);
    int update(RepairWorker value);
    WorkerDetailResponse selectDetail(@Param("id") Long id);
    List<WorkerDetailResponse> selectPage(@Param("workerNo") String workerNo,@Param("realName") String realName,
        @Param("phone") String phone,@Param("workStatus") Integer workStatus,@Param("faultTypeId") Long faultTypeId);
    int countSkillWorkers(@Param("faultTypeId") Long faultTypeId,@Param("excluded") List<Long> excluded);
    int countAreaWorkers(@Param("faultTypeId") Long faultTypeId,@Param("campusId") Long campusId,@Param("areaId") Long areaId,
                         @Param("buildingId") Long buildingId,@Param("excluded") List<Long> excluded);
    List<DispatchCandidate> selectDispatchCandidates(@Param("faultTypeId") Long faultTypeId,@Param("campusId") Long campusId,
        @Param("areaId") Long areaId,@Param("buildingId") Long buildingId,@Param("now") LocalDateTime now,@Param("excluded") List<Long> excluded);
}
