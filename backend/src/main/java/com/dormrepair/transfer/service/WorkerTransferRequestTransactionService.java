package com.dormrepair.transfer.service;

import com.dormrepair.domain.entity.RepairTransferRequest;
import com.dormrepair.domain.entity.SysIdempotentRecord;
import com.dormrepair.domain.entity.SysOperationLog;
import com.dormrepair.domain.mapper.RepairTransferRequestMapper;
import com.dormrepair.domain.mapper.SysIdempotentRecordMapper;
import com.dormrepair.domain.mapper.SysOperationLogMapper;
import com.dormrepair.security.model.LoginUser;
import com.dormrepair.transfer.dto.CreateTransferRequest;
import com.dormrepair.transfer.enums.TransferApprovalStatus;
import com.dormrepair.transfer.enums.TransferExecuteStatus;
import com.dormrepair.transfer.vo.CreateTransferResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WorkerTransferRequestTransactionService {
    private final SysIdempotentRecordMapper idempotency;
    private final RepairTransferRequestMapper transfers;
    private final SysOperationLogMapper logs;

    public WorkerTransferRequestTransactionService(SysIdempotentRecordMapper idempotency,
                                                   RepairTransferRequestMapper transfers,
                                                   SysOperationLogMapper logs) {
        this.idempotency = idempotency; this.transfers = transfers; this.logs = logs;
    }

    @Transactional
    public CreateTransferResponse create(Long orderId, Long workerId, LoginUser user,
                                         CreateTransferRequest request, String hash) {
        LocalDateTime now = LocalDateTime.now();
        SysIdempotentRecord record = new SysIdempotentRecord();
        record.setBizNo(request.bizNo()); record.setBizType(WorkerTransferRequestService.BIZ_TYPE);
        record.setUserId(user.getUserId()); record.setRequestHash(hash); record.setStatus(0); record.setExpireTime(now.plusDays(1));
        idempotency.insert(record);

        RepairTransferRequest transfer = new RepairTransferRequest();
        transfer.setOrderId(orderId); transfer.setApplicantWorkerId(workerId); transfer.setOriginalAssigneeId(workerId);
        transfer.setReasonType(request.reasonType().code()); transfer.setReasonDescription(request.reason());
        transfer.setApprovalStatus(TransferApprovalStatus.PENDING.code());
        transfer.setExecuteStatus(TransferExecuteStatus.NOT_EXECUTED.code()); transfer.setDeleted(0);
        transfers.insert(transfer);

        SysOperationLog log = new SysOperationLog();
        log.setUserId(user.getUserId()); log.setUsername(user.getUsername()); log.setRoleType(user.getRoleType().getCode());
        log.setModuleName("维修转派"); log.setOperationName("申请转派"); log.setBusinessType("TRANSFER_REQUEST");
        log.setBusinessId(transfer.getId()); log.setRequestMethod("POST");
        log.setRequestUri("/api/worker/repair-orders/" + orderId + "/transfer-requests");
        log.setResultStatus(1); log.setOperationTime(now); logs.insert(log);
        idempotency.markSuccess(record.getId(), String.valueOf(transfer.getId()));
        return new CreateTransferResponse(transfer.getId(), false);
    }
}
