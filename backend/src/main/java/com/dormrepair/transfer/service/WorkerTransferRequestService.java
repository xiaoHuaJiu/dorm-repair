package com.dormrepair.transfer.service;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairOrder;
import com.dormrepair.domain.entity.SysIdempotentRecord;
import com.dormrepair.domain.mapper.RepairOrderMapper;
import com.dormrepair.domain.mapper.RepairTransferRequestMapper;
import com.dormrepair.domain.mapper.SysIdempotentRecordMapper;
import com.dormrepair.order.service.CurrentWorkerResolver;
import com.dormrepair.security.context.UserContext;
import com.dormrepair.security.model.LoginUser;
import com.dormrepair.transfer.dto.CreateTransferRequest;
import com.dormrepair.transfer.dto.TransferQueryRequest;
import com.dormrepair.transfer.enums.TransferApprovalStatus;
import com.dormrepair.transfer.enums.TransferExecuteStatus;
import com.dormrepair.transfer.vo.CreateTransferResponse;
import com.dormrepair.transfer.vo.TransferRequestView;
import com.dormrepair.common.result.PageResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

@Service
public class WorkerTransferRequestService {
    static final String BIZ_TYPE = "TRANSFER_CREATE";
    private static final Set<Integer> ALLOWED_STATUSES = Set.of(1, 2, 4, 5);

    private final RepairOrderMapper orders;
    private final RepairTransferRequestMapper transfers;
    private final SysIdempotentRecordMapper idempotency;
    private final CurrentWorkerResolver workerResolver;
    private final WorkerTransferRequestTransactionService transactions;
    private final ObjectMapper json;

    public WorkerTransferRequestService(RepairOrderMapper orders, RepairTransferRequestMapper transfers,
                                        SysIdempotentRecordMapper idempotency, CurrentWorkerResolver workerResolver,
                                        WorkerTransferRequestTransactionService transactions, ObjectMapper json) {
        this.orders = orders;
        this.transfers = transfers;
        this.idempotency = idempotency;
        this.workerResolver = workerResolver;
        this.transactions = transactions;
        this.json = json;
    }

    public CreateTransferResponse create(Long orderId, CreateTransferRequest request) {
        LoginUser user = UserContext.getCurrentUser();
        Long workerId = workerResolver.resolveCurrentWorkerId();
        String hash = hash(orderId, request);
        SysIdempotentRecord existing = idempotency.selectByBusinessKey(BIZ_TYPE, user.getUserId(), request.bizNo());
        if (existing != null) {
            if (!hash.equals(existing.getRequestHash())) throw new BusinessException(ResultCodeEnum.TRANSFER_BIZ_NO_CONFLICT);
            if (Integer.valueOf(1).equals(existing.getStatus())) {
                return new CreateTransferResponse(Long.valueOf(existing.getResultSnapshot()), true);
            }
            throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);
        }

        RepairOrder order = orders.selectById(orderId);
        if (order == null || Integer.valueOf(1).equals(order.getDeleted())) throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        if (!workerId.equals(order.getCurrentAssigneeId())) throw new BusinessException(ResultCodeEnum.ORDER_ACCESS_DENIED);
        if (!ALLOWED_STATUSES.contains(order.getStatus())) throw new BusinessException(ResultCodeEnum.TRANSFER_STATE_INVALID);
        if (transfers.selectPendingByOrderId(orderId) != null) throw new BusinessException(ResultCodeEnum.TRANSFER_PENDING_EXISTS);

        try { return transactions.create(orderId, workerId, user, request, hash); }
        catch (DuplicateKeyException e) {
            SysIdempotentRecord raced = idempotency.selectByBusinessKey(BIZ_TYPE, user.getUserId(), request.bizNo());
            if (raced != null && hash.equals(raced.getRequestHash()) && Integer.valueOf(1).equals(raced.getStatus()))
                return new CreateTransferResponse(Long.valueOf(raced.getResultSnapshot()), true);
            throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);
        }
    }

    public PageResult<TransferRequestView> page(TransferQueryRequest query) {
        Long workerId = workerResolver.resolveCurrentWorkerId();
        query.setApplicantWorkerId(null);
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        return PageResult.from(new PageInfo<>(transfers.selectPage(query, workerId)));
    }

    public TransferRequestView detail(Long id) {
        Long workerId = workerResolver.resolveCurrentWorkerId();
        TransferRequestView view = transfers.selectViewById(id);
        if (view == null) throw new BusinessException(ResultCodeEnum.TRANSFER_NOT_FOUND);
        if (!workerId.equals(view.getApplicantWorkerId())) throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        return view;
    }

    private String hash(Long orderId, CreateTransferRequest request) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(json.writeValueAsBytes(new Object[]{orderId, request.reasonType(), request.reason()}));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("转派请求摘要生成失败", e);
        }
    }
}
