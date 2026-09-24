package com.dormrepair.leave.service;

import com.dormrepair.leave.dto.CreateLeaveRequestDTO;
import com.dormrepair.leave.dto.ReviewLeaveRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.LinkedHashMap;

/**
 * 请假创建与审批请求摘要计算，用于 bizNo 幂等校验。
 * 同一 bizNo 携带不同内容时，摘要不一致会触发冲突拒绝。
 */
@Component
public class LeaveRequestHasher {
    private final ObjectMapper json = new ObjectMapper();

    public String hashCreate(Long workerId, CreateLeaveRequestDTO request) {
        try {
            var values = new LinkedHashMap<String, Object>();
            values.put("workerId", workerId);
            values.put("startTime", request.startTime().toString());
            values.put("endTime", request.endTime().toString());
            values.put("reason", request.reason());
            return digest(values);
        } catch (Exception e) {
            throw new IllegalStateException("无法计算请假申请摘要", e);
        }
    }

    public String hashReview(Long adminUid, Long leaveId, ReviewLeaveRequestDTO request) {
        try {
            var values = new LinkedHashMap<String, Object>();
            values.put("adminUid", adminUid);
            values.put("leaveId", leaveId);
            values.put("action", request.action());
            values.put("remark", request.remark());
            return digest(values);
        } catch (Exception e) {
            throw new IllegalStateException("无法计算请假审批摘要", e);
        }
    }

    private String digest(Object value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(value));
        return java.util.HexFormat.of().formatHex(bytes);
    }
}
