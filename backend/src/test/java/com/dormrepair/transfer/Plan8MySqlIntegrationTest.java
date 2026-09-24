package com.dormrepair.transfer;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.security.model.LoginUser;
import com.dormrepair.transfer.dto.CreateTransferRequest;
import com.dormrepair.transfer.dto.ReviewTransferRequest;
import com.dormrepair.transfer.enums.TransferReasonType;
import com.dormrepair.transfer.enums.TransferReviewAction;
import com.dormrepair.transfer.service.AdminTransferRequestService;
import com.dormrepair.transfer.service.WorkerTransferRequestService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@Transactional
class Plan8MySqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WorkerTransferRequestService workerService;
    @Autowired AdminTransferRequestService adminService;

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void createsReplaysAndApprovesWithDifferentWorker() {
        Fixture f = fixture();
        login(f.workerUserA(), UserRoleEnum.WORKER);
        var created = workerService.create(f.order(), new CreateTransferRequest("p8-create-" + f.suffix(), TransferReasonType.NO_SKILL, "需要其他工种"));
        var replayed = workerService.create(f.order(), new CreateTransferRequest("p8-create-" + f.suffix(), TransferReasonType.NO_SKILL, "需要其他工种"));
        assertThat(replayed.requestId()).isEqualTo(created.requestId());
        assertThat(replayed.replayed()).isTrue();
        assertThat(jdbc.queryForMap("SELECT status,current_assignee_id FROM repair_order WHERE id=?", f.order()))
            .containsEntry("status", 2).containsEntry("current_assignee_id", f.workerA());

        login(f.adminUser(), UserRoleEnum.ADMIN);
        var reviewed = adminService.review(created.requestId(), new ReviewTransferRequest("p8-review-" + f.suffix(), TransferReviewAction.APPROVE, "同意"));
        assertThat(reviewed.reviewSuccess()).isTrue();
        assertThat(reviewed.dispatchSuccess()).isTrue();
        assertThat(reviewed.newAssigneeId()).isEqualTo(f.workerB());
        assertThat(jdbc.queryForMap("SELECT status,current_assignee_id FROM repair_order WHERE id=?", f.order()))
            .containsEntry("status", 1).containsEntry("current_assignee_id", f.workerB());
        assertThat(jdbc.queryForMap("SELECT approval_status,execute_status,new_assignee_id FROM repair_transfer_request WHERE id=?", created.requestId()))
            .containsEntry("approval_status", 1).containsEntry("execute_status", 2).containsEntry("new_assignee_id", f.workerB());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_order_flow WHERE order_id=? AND source_type=3", Integer.class, f.order())).isEqualTo(1);
    }

    private Fixture fixture() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        long b = 98_000_000_000L + Integer.toUnsignedLong(s.hashCode()) * 30L;
        long student = b + 1, workerUserA = b + 2, workerUserB = b + 3, admin = b + 4;
        long workerA = b + 5, workerB = b + 6, campus = b + 7, area = b + 8, building = b + 9, room = b + 10, fault = b + 11, order = b + 12;
        user(student, "p8s_" + s, 1); user(workerUserA, "p8a_" + s, 2); user(workerUserB, "p8b_" + s, 2); user(admin, "p8m_" + s, 3);
        jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)", workerA, workerUserA, "P8A" + s);
        jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)", workerB, workerUserB, "P8B" + s);
        jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)", fault, "P8F" + s, "阶段八故障");
        area(campus, 0, "P8C" + s, 1); area(area, campus, "P8A" + s, 2); area(building, area, "P8B" + s, 3); area(room, building, "P8R" + s, 4);
        for (long worker : new long[]{workerA, workerB}) {
            jdbc.update("INSERT INTO repair_worker_fault_type(worker_id,fault_type_id,status) VALUES(?,?,1)", worker, fault);
            jdbc.update("INSERT INTO repair_worker_area_scope(worker_id,campus_id,area_id,building_id,status) VALUES(?,?,?,?,1)", worker, campus, area, building);
        }
        jdbc.update("INSERT INTO repair_work_schedule(schedule_name,start_date,end_date,work_start_time,work_end_time,status) VALUES(?,CURRENT_DATE,CURRENT_DATE,?,?,1)", "P8" + s, "00:00:00", "23:59:59");
        jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,report_time,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),0)", order, "P8O" + s, student, "测试", "13800138000", campus, area, building, room, fault, "PLAN8测试", 2, workerA);
        return new Fixture(s, workerUserA, admin, workerA, workerB, order);
    }

    private void user(long id, String name, int role) { jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试',?,1,0)", id, name, "x", role); }
    private void area(long id, long parent, String code, int type) { jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)", id, parent, code, code, type); }
    private void login(long userId, UserRoleEnum role) {
        LoginUser user = new LoginUser(userId, "plan8", "测试", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    private record Fixture(String suffix, long workerUserA, long adminUser, long workerA, long workerB, long order) {}
}
