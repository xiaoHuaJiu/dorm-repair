package com.dormrepair.transfer.service;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.common.result.PageResult;
import com.dormrepair.dispatch.model.DispatchResult;
import com.dormrepair.dispatch.model.DispatchSourceType;
import com.dormrepair.dispatch.service.RepairDispatchService;
import com.dormrepair.domain.entity.*;
import com.dormrepair.domain.mapper.*;
import com.dormrepair.security.context.UserContext;
import com.dormrepair.security.model.LoginUser;
import com.dormrepair.transfer.dto.ReviewTransferRequest;
import com.dormrepair.transfer.dto.TransferQueryRequest;
import com.dormrepair.transfer.enums.*;
import com.dormrepair.transfer.vo.TransferRequestView;
import com.dormrepair.transfer.vo.TransferReviewResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;

@Service
public class AdminTransferRequestService {
    private static final String BIZ_TYPE = "TRANSFER_REVIEW";
    private static final Set<Integer> ALLOWED_STATUSES = Set.of(1, 2, 4, 5);
    private final RepairTransferRequestMapper transfers;
    private final RepairOrderMapper orders;
    private final SysIdempotentRecordMapper idempotency;
    private final SysOperationLogMapper logs;
    private final RepairDispatchService dispatch;
    private final ObjectMapper json;

    public AdminTransferRequestService(RepairTransferRequestMapper transfers, RepairOrderMapper orders,
                                       SysIdempotentRecordMapper idempotency, SysOperationLogMapper logs,
                                       RepairDispatchService dispatch, ObjectMapper json) {
        this.transfers = transfers; this.orders = orders; this.idempotency = idempotency;
        this.logs = logs; this.dispatch = dispatch; this.json = json;
    }

    public PageResult<TransferRequestView> page(TransferQueryRequest query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        return PageResult.from(new PageInfo<>(transfers.selectPage(query, null)));
    }

    public TransferRequestView detail(Long id) {
        TransferRequestView view = transfers.selectViewById(id);
        if (view == null) throw new BusinessException(ResultCodeEnum.TRANSFER_NOT_FOUND);
        return view;
    }

    @Transactional
    public TransferReviewResponse review(Long id, ReviewTransferRequest request) {
        LoginUser admin = UserContext.getCurrentUser();
        String hash = hash(id, request);
        SysIdempotentRecord existing = idempotency.selectByBusinessKey(BIZ_TYPE, admin.getUserId(), request.bizNo());
        if (existing != null) {
            if (!hash.equals(existing.getRequestHash())) throw new BusinessException(ResultCodeEnum.TRANSFER_BIZ_NO_CONFLICT);
            if (Integer.valueOf(1).equals(existing.getStatus())) return replay(existing.getResultSnapshot());
            throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);
        }

        RepairTransferRequest transfer = transfers.selectByIdForUpdate(id);
        if (transfer == null) throw new BusinessException(ResultCodeEnum.TRANSFER_NOT_FOUND);
        existing = idempotency.selectByBusinessKey(BIZ_TYPE, admin.getUserId(), request.bizNo());
        if (existing != null) {
            if (!hash.equals(existing.getRequestHash())) throw new BusinessException(ResultCodeEnum.TRANSFER_BIZ_NO_CONFLICT);
            if (Integer.valueOf(1).equals(existing.getStatus())) return replay(existing.getResultSnapshot());
            throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);
        }
        if (!Integer.valueOf(TransferApprovalStatus.PENDING.code()).equals(transfer.getApprovalStatus()))
            throw new BusinessException(ResultCodeEnum.TRANSFER_ALREADY_REVIEWED);

        LocalDateTime now = LocalDateTime.now();
        SysIdempotentRecord record = new SysIdempotentRecord();
        record.setBizNo(request.bizNo()); record.setBizType(BIZ_TYPE); record.setUserId(admin.getUserId());
        record.setRequestHash(hash); record.setStatus(0); record.setExpireTime(now.plusDays(1)); idempotency.insert(record);

        RepairOrder order = orders.selectById(transfer.getOrderId());
        if (order == null || !transfer.getOriginalAssigneeId().equals(order.getCurrentAssigneeId())
            || !ALLOWED_STATUSES.contains(order.getStatus())) {
            transfers.casReview(id, TransferApprovalStatus.INVALID.code(), admin.getUserId(), "工单负责人或状态已变化", now);
            throw new BusinessException(ResultCodeEnum.TRANSFER_ORDER_CHANGED);
        }

        TransferReviewResponse response;
        if (request.action() == TransferReviewAction.REJECT) {
            ensureReviewed(transfers.casReview(id, TransferApprovalStatus.REJECTED.code(), admin.getUserId(), request.remark(), now));
            response = new TransferReviewResponse(id, true, false, null, null, false);
        } else {
            ensureReviewed(transfers.casReview(id, TransferApprovalStatus.APPROVED.code(), admin.getUserId(), request.remark(), now));
            DispatchResult result = dispatch.dispatch(order.getId(), Set.of(transfer.getOriginalAssigneeId()), DispatchSourceType.TRANSFER_REASSIGN);
            String failure = result.failureReason() == null ? null : result.failureReason().name();
            if (transfers.markExecution(id, result.success() ? TransferExecuteStatus.SUCCESS.code() : TransferExecuteStatus.FAILED.code(),
                result.workerId(), failure, LocalDateTime.now()) != 1) throw new IllegalStateException("转派执行结果回写失败");
            response = new TransferReviewResponse(id, true, result.success(), result.workerId(), failure, false);
        }

        writeLog(admin, id, request.action(), now);
        try { idempotency.markSuccess(record.getId(), json.writeValueAsString(response)); }
        catch (Exception e) { throw new IllegalStateException("转派审批结果序列化失败", e); }
        return response;
    }

    private void ensureReviewed(int rows) {
        if (rows != 1) throw new BusinessException(ResultCodeEnum.TRANSFER_ALREADY_REVIEWED);
    }

    private void writeLog(LoginUser admin, Long id, TransferReviewAction action, LocalDateTime now) {
        SysOperationLog log = new SysOperationLog(); log.setUserId(admin.getUserId()); log.setUsername(admin.getUsername());
        log.setRoleType(admin.getRoleType().getCode()); log.setModuleName("维修转派");
        log.setOperationName(action == TransferReviewAction.APPROVE ? "审批通过并重新派单" : "驳回转派申请");
        log.setBusinessType("TRANSFER_REQUEST"); log.setBusinessId(id); log.setRequestMethod("POST");
        log.setRequestUri("/api/admin/transfer-requests/" + id + "/review"); log.setResultStatus(1); log.setOperationTime(now);
        logs.insert(log);
    }

    private String hash(Long id, ReviewTransferRequest request) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(json.writeValueAsBytes(new Object[]{id, request.action(), request.remark()}))); }
        catch (Exception e) { throw new IllegalStateException("转派审批请求摘要生成失败", e); }
    }

    private TransferReviewResponse replay(String snapshot) {
        try {
            TransferReviewResponse saved = json.readValue(snapshot, TransferReviewResponse.class);
            return new TransferReviewResponse(saved.requestId(), saved.reviewSuccess(), saved.dispatchSuccess(),
                saved.newAssigneeId(), saved.failureReason(), true);
        } catch (Exception e) { throw new IllegalStateException("转派审批结果读取失败", e); }
    }
}
