package com.dormrepair.transfer;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.security.model.LoginUser;
import com.dormrepair.transfer.dto.ReviewTransferRequest;
import com.dormrepair.transfer.enums.TransferReviewAction;
import com.dormrepair.transfer.service.AdminTransferRequestService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.sql.init.mode=never")
class Plan8ConcurrencyMySqlTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AdminTransferRequestService service;
    Fixture fixture;

    @AfterEach void cleanup() {
        if (fixture == null) return;
        jdbc.update("DELETE FROM sys_operation_log WHERE business_id=? AND business_type='TRANSFER_REQUEST'", fixture.request());
        jdbc.update("DELETE FROM sys_idempotent_record WHERE biz_type='TRANSFER_REVIEW' AND user_id IN (?,?)", fixture.adminA(), fixture.adminB());
        jdbc.update("DELETE FROM repair_transfer_request WHERE id=?", fixture.request());
        jdbc.update("DELETE FROM repair_order WHERE id=?", fixture.order());
        jdbc.update("DELETE FROM repair_worker WHERE id=?", fixture.worker());
        jdbc.update("DELETE FROM sys_user WHERE id IN (?,?,?,?)", fixture.student(), fixture.workerUser(), fixture.adminA(), fixture.adminB());
    }

    @Test void concurrentReviewAllowsExactlyOneDecision() throws Exception {
        fixture = createFixture();
        AtomicInteger success = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<?> a = pool.submit(() -> review(start, fixture.adminA(), "p8-review-a-" + fixture.suffix(), success));
        Future<?> b = pool.submit(() -> review(start, fixture.adminB(), "p8-review-b-" + fixture.suffix(), success));
        start.countDown(); a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS); pool.shutdownNow();
        assertThat(success.get()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT approval_status FROM repair_transfer_request WHERE id=?", Integer.class, fixture.request())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_operation_log WHERE business_id=? AND business_type='TRANSFER_REQUEST'", Integer.class, fixture.request())).isEqualTo(1);
    }

    private void review(CountDownLatch start, long admin, String bizNo, AtomicInteger success) {
        try {
            start.await(); login(admin);
            service.review(fixture.request(), new ReviewTransferRequest(bizNo, TransferReviewAction.REJECT, "并发审批"));
            success.incrementAndGet();
        } catch (Exception ignored) {
        } finally { SecurityContextHolder.clearContext(); }
    }

    private Fixture createFixture() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        long b = 99_000_000_000L + Integer.toUnsignedLong(s.hashCode()) * 20L;
        long student = b + 1, workerUser = b + 2, adminA = b + 3, adminB = b + 4, worker = b + 5, order = b + 6, request = b + 7;
        user(student, "p8cs_" + s, 1); user(workerUser, "p8cw_" + s, 2); user(adminA, "p8ca_" + s, 3); user(adminB, "p8cb_" + s, 3);
        jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)", worker, workerUser, "P8CW" + s);
        jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,report_time,deleted) VALUES(?,?,?,'测试','13800138000',1,1,1,1,1,'PLAN8并发审批',?,?,NOW(),0)", order, "P8CO" + s, student, 2, worker);
        jdbc.update("INSERT INTO repair_transfer_request(id,order_id,applicant_worker_id,original_assignee_id,reason_type,reason_description,approval_status,execute_status,deleted) VALUES(?,?,?,?,2,'并发审批测试',0,0,0)", request, order, worker, worker);
        return new Fixture(s, student, workerUser, adminA, adminB, worker, order, request);
    }

    private void user(long id, String name, int role) { jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试',?,1,0)", id, name, "x", role); }
    private void login(long id) {
        LoginUser user = new LoginUser(id, "plan8-admin", "管理员", UserRoleEnum.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    private record Fixture(String suffix, long student, long workerUser, long adminA, long adminB, long worker, long order, long request) {}
}
