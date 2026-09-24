package com.dormrepair.leave;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.leave.dto.CreateLeaveRequestDTO;
import com.dormrepair.leave.dto.ReviewLeaveRequestDTO;
import com.dormrepair.leave.service.LeaveEffectiveService;
import com.dormrepair.leave.service.LeaveEndService;
import com.dormrepair.leave.service.LeaveRequestService;
import com.dormrepair.leave.service.LeaveReviewService;
import com.dormrepair.leave.vo.CreateLeaveRequestResponse;
import com.dormrepair.security.model.LoginUser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PLAN-9 请假全链路 MySQL 集成测试（需要真实 MySQL，暂未运行）：
 * 创建与重叠校验、审批、请假生效与转派、幂等、请假结束恢复、连续请假。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@Transactional
class Plan9MySqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired LeaveRequestService leaveService;
    @Autowired LeaveReviewService reviewService;
    @Autowired LeaveEffectiveService effectiveService;
    @Autowired LeaveEndService endService;

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void overlappingPendingLeaveIsRejected() {
        Fixture f = fixture();
        loginWorker(f.workerUser);
        CreateLeaveRequestDTO first = request("biz-o1", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        CreateLeaveRequestResponse created = leaveService.create(first);
        assertThat(created.leaveId()).isNotNull();
        CreateLeaveRequestDTO second = request("biz-o2", LocalDateTime.now().plusDays(1).plusHours(1), LocalDateTime.now().plusDays(3));
        assertThatThrownBy(() -> leaveService.create(second)).isInstanceOf(BusinessException.class);
        // 首尾相接（[start,end) 边界）不重叠
        CreateLeaveRequestDTO adjacent = request("biz-o3", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3));
        assertThat(leaveService.create(adjacent).leaveId()).isNotNull();
    }

    @Test
    void rejectedLeaveDoesNotBlockNewApplication() {
        Fixture f = fixture();
        loginWorker(f.workerUser);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        CreateLeaveRequestResponse created = leaveService.create(request("biz-r1", start, end));
        loginAdmin(f.adminUser);
        reviewService.review(created.leaveId(), new ReviewLeaveRequestDTO("biz-r2", "REJECT", "不批准"));
        assertThat(jdbc.queryForObject("SELECT status FROM repair_leave_request WHERE id=?", Integer.class, created.leaveId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT work_status FROM repair_worker WHERE id=?", Integer.class, f.worker)).isEqualTo(0);
        loginWorker(f.workerUser);
        // 同一时间段重新申请应被允许
        assertThat(leaveService.create(request("biz-r3", start, end)).leaveId()).isNotNull();
    }

    @Test
    void approveFutureLeaveKeepsWorkerNormalUntilEffective() {
        Fixture f = fixture();
        loginWorker(f.workerUser);
        CreateLeaveRequestResponse created = leaveService.create(
            request("biz-f1", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)));
        loginAdmin(f.adminUser);
        reviewService.review(created.leaveId(), new ReviewLeaveRequestDTO("biz-f2", "APPROVE", "同意"));
        assertThat(jdbc.queryForObject("SELECT status FROM repair_leave_request WHERE id=?", Integer.class, created.leaveId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT work_status FROM repair_worker WHERE id=?", Integer.class, f.worker)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT reassign_status FROM repair_leave_request WHERE id=?", Integer.class, created.leaveId())).isEqualTo(0);
    }

    @Test
    void effectiveTaskMovesWorkerOnLeaveAndReassignsOrders() {
        Fixture f = fixture();
        loginWorker(f.workerUser);
        CreateLeaveRequestResponse created = leaveService.create(
            request("biz-e1", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusDays(1)));
        loginAdmin(f.adminUser);
        reviewService.review(created.leaveId(), new ReviewLeaveRequestDTO("biz-e2", "APPROVE", "同意"));
        jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,report_time,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),0)",
            f.order, "P9O" + f.suffix, f.studentUser, "测试", "13800138000", f.campus, f.area, f.building, f.room, f.fault, "请假转派", 2, f.worker);
        effectiveService.processDueLeaves();
        assertThat(jdbc.queryForObject("SELECT work_status FROM repair_worker WHERE id=?", Integer.class, f.worker)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT reassign_status FROM repair_leave_request WHERE id=?", Integer.class, created.leaveId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT current_assignee_id FROM repair_order WHERE id=?", Long.class, f.order)).isEqualTo(f.workerB);
        assertThat(jdbc.queryForObject("SELECT status FROM repair_order WHERE id=?", Integer.class, f.order)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_order_flow WHERE order_id=? AND source_type=4", Integer.class, f.order)).isEqualTo(1);
    }

    @Test
    void effectiveTaskIsIdempotentAcrossRepeatedScans() {
        Fixture f = fixture();
        loginWorker(f.workerUser);
        CreateLeaveRequestResponse created = leaveService.create(
            request("biz-i1", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusDays(1)));
        loginAdmin(f.adminUser);
        reviewService.review(created.leaveId(), new ReviewLeaveRequestDTO("biz-i2", "APPROVE", "同意"));
        effectiveService.processDueLeaves();
        effectiveService.processDueLeaves();
        effectiveService.processDueLeaves();
        assertThat(jdbc.queryForObject("SELECT reassign_status FROM repair_leave_request WHERE id=?", Integer.class, created.leaveId())).isEqualTo(2);
    }

    @Test
    void partialFailureMarksPartialFailedAndWritesSummaryAlert() {
        Fixture f = fixture();
        loginWorker(f.workerUser);
        CreateLeaveRequestResponse created = leaveService.create(
            request("biz-p1", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusDays(1)));
        loginAdmin(f.adminUser);
        reviewService.review(created.leaveId(), new ReviewLeaveRequestDTO("biz-p2", "APPROVE", "同意"));
        // 工单的故障类型没有任何候选（workerB 未配置该技能），转派必然失败
        jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)", f.fault2, "P9F2" + f.suffix, "无候选人故障");
        jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,report_time,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),0)",
            f.order, "P9P" + f.suffix, f.studentUser, "测试", "13800138000", f.campus, f.area, f.building, f.room, f.fault2, "部分失败", 2, f.worker);
        effectiveService.processDueLeaves();
        assertThat(jdbc.queryForObject("SELECT reassign_status FROM repair_leave_request WHERE id=?", Integer.class, created.leaveId())).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT status FROM repair_order WHERE id=?", Integer.class, f.order)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT current_assignee_id FROM repair_order WHERE id=?", Long.class, f.order)).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_dispatch_alert WHERE source_type=4 AND failure_reason='LEAVE_REASSIGN_PARTIAL_FAILED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void endTaskRestoresWorkerAfterLeaveEnds() {
        Fixture f = fixture();
        loginWorker(f.workerUser);
        CreateLeaveRequestResponse created = leaveService.create(
            request("biz-n1", LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)));
        loginAdmin(f.adminUser);
        reviewService.review(created.leaveId(), new ReviewLeaveRequestDTO("biz-n2", "APPROVE", "同意"));
        effectiveService.processDueLeaves();
        assertThat(jdbc.queryForObject("SELECT work_status FROM repair_worker WHERE id=?", Integer.class, f.worker)).isEqualTo(1);
        endService.processEndedLeaves();
        assertThat(jdbc.queryForObject("SELECT work_status FROM repair_worker WHERE id=?", Integer.class, f.worker)).isEqualTo(0);
    }

    @Test
    void backToBackLeaveKeepsWorkerOnLeave() {
        Fixture f = fixture();
        loginWorker(f.workerUser);
        // 请假 A：昨天到今天 8 点（已结束）
        LocalDateTime today8 = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);
        CreateLeaveRequestResponse a = leaveService.create(request("biz-b1", today8.minusDays(1), today8));
        // 请假 B：今天 8 点开始（已生效）
        CreateLeaveRequestResponse b = leaveService.create(request("biz-b2", today8, today8.plusDays(2)));
        loginAdmin(f.adminUser);
        reviewService.review(a.leaveId(), new ReviewLeaveRequestDTO("biz-b3", "APPROVE", "同意"));
        reviewService.review(b.leaveId(), new ReviewLeaveRequestDTO("biz-b4", "APPROVE", "同意"));
        effectiveService.processDueLeaves();
        assertThat(jdbc.queryForObject("SELECT work_status FROM repair_worker WHERE id=?", Integer.class, f.worker)).isEqualTo(1);
        endService.processEndedLeaves();
        // A 结束但 B 生效中，保持请假中
        assertThat(jdbc.queryForObject("SELECT work_status FROM repair_worker WHERE id=?", Integer.class, f.worker)).isEqualTo(1);
    }

    private void loginWorker(long userId) {
        LoginUser user = new LoginUser(userId, "p9w_" + UUID.randomUUID(), UserRoleEnum.WORKER);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private void loginAdmin(long userId) {
        LoginUser user = new LoginUser(userId, "p9a_" + UUID.randomUUID(), UserRoleEnum.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private CreateLeaveRequestDTO request(String bizNo, LocalDateTime start, LocalDateTime end) {
        return new CreateLeaveRequestDTO(bizNo, start, end, "集成测试请假");
    }

    private Fixture fixture() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        long b = 9_800_000_000L + Math.abs(s.hashCode()) * 20L;
        long adminUser = b + 1, workerUser = b + 2, workerBUser = b + 3, studentUser = b + 4;
        long worker = b + 5, workerB = b + 6, campus = b + 7, area = b + 8, building = b + 9, room = b + 10, fault = b + 11, fault2 = b + 12, order = b + 13;
        jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'管理员',3,1,0)", adminUser, "p9ad_" + s, "x");
        jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'请假维修员',2,1,0)", workerUser, "p9w_" + s, "x");
        jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'替补维修员',2,1,0)", workerBUser, "p9wb_" + s, "x");
        jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'学生',1,1,0)", studentUser, "p9s_" + s, "x");
        jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)", worker, workerUser, "P9W" + s);
        jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)", workerB, workerBUser, "P9WB" + s);
        jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)", fault, "P9F" + s, "阶段九故障");
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)", campus, 0L, "P9C" + s, "P9C" + s, 1);
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)", area, campus, "P9A" + s, "P9A" + s, 2);
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)", building, area, "P9B" + s, "P9B" + s, 3);
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)", room, building, "P9R" + s, "P9R" + s, 4);
        for (long w : new long[] {worker, workerB}) {
            jdbc.update("INSERT INTO repair_worker_fault_type(worker_id,fault_type_id,status) VALUES(?,?,1)", w, fault);
            jdbc.update("INSERT INTO repair_worker_area_scope(worker_id,campus_id,area_id,building_id,status) VALUES(?,?,?,?,1)", w, campus, area, building);
        }
        jdbc.update("INSERT INTO repair_work_schedule(schedule_name,start_date,end_date,work_start_time,work_end_time,status) VALUES(?,CURRENT_DATE,CURRENT_DATE,?,?,1)", "P9" + s, "00:00:00", "23:59:59");
        return new Fixture(s, adminUser, workerUser, studentUser, worker, workerB, campus, area, building, room, fault, fault2, order);
    }

    private record Fixture(String suffix, long adminUser, long workerUser, long studentUser, long worker, long workerB,
        long campus, long area, long building, long room, long fault, long fault2, long order) {}
}
