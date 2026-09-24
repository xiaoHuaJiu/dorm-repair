package com.dormrepair.leave;

import com.dormrepair.domain.mapper.RepairLeaveRequestMapper;
import com.dormrepair.leave.service.LeaveEffectiveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLAN-9 请假并发 MySQL 测试（需要真实 MySQL，暂未运行）：
 * 审批 CAS 并发与请假生效任务 CAS 抢任务。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
class Plan9ConcurrencyMySqlTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RepairLeaveRequestMapper leaves;
    @Autowired LeaveEffectiveService effectiveService;

    @Test
    void concurrentReviewOnlyOneCasUpdateSucceeds() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        long b = 9_900_000_000L + Math.abs(s.hashCode()) * 20L;
        long adminA = b + 1, adminB = b + 2, workerUser = b + 3, worker = b + 4, leaveId = b + 5;
        try {
            jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'管理员',3,1,0)", adminA, "p9ca_" + s, "x");
            jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'管理员',3,1,0)", adminB, "p9cb_" + s, "x");
            jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'维修员',2,1,0)", workerUser, "p9cw_" + s, "x");
            jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)", worker, workerUser, "P9CW" + s);
            jdbc.update("INSERT INTO repair_leave_request(id,worker_id,start_time,end_time,reason,status,reassign_status,deleted) VALUES(?,?,?,?,?,0,0,0)",
                leaveId, worker, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "并发审批", "x".substring(0, 0));
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
            Callable<Integer> task = () -> {
                ready.countDown(); start.await();
                return leaves.casReview(leaveId, 0, 1, adminA, "同意", LocalDateTime.now());
            };
            Future<Integer> a = pool.submit(task), c = pool.submit(task);
            ready.await(5, TimeUnit.SECONDS); start.countDown();
            assertThat(a.get() + c.get()).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT status FROM repair_leave_request WHERE id=?", Integer.class, leaveId)).isEqualTo(1);
            pool.shutdownNow();
        } finally {
            jdbc.update("DELETE FROM repair_leave_request WHERE id=?", leaveId);
            jdbc.update("DELETE FROM repair_worker WHERE id=?", worker);
            jdbc.update("DELETE FROM sys_user WHERE id IN (?,?,?)", adminA, adminB, workerUser);
        }
    }

    @Test
    void concurrentEffectiveProcessOnlyOneWinsCasTake() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        long b = 9_900_100_000L + Math.abs(s.hashCode()) * 20L;
        long workerUser = b + 1, worker = b + 2, leaveId = b + 3;
        try {
            jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'维修员',2,1,0)", workerUser, "p9ew_" + s, "x");
            jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)", worker, workerUser, "P9EW" + s);
            jdbc.update("INSERT INTO repair_leave_request(id,worker_id,start_time,end_time,reason,status,reassign_status,deleted) VALUES(?,?,?,?,?,1,0,0)",
                leaveId, worker, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(1), "并发生效");
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime staleBefore = now.minusMinutes(5);
            Runnable task = () -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException ignored) { return; }
                effectiveService.processOne(leaveId, now, staleBefore);
            };
            pool.submit(task); pool.submit(task);
            ready.await(5, TimeUnit.SECONDS); start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
            // 最终状态只能是已完成或部分失败，不会停留在处理中；work_status 只被更新为请假中
            Integer reassign = jdbc.queryForObject("SELECT reassign_status FROM repair_leave_request WHERE id=?", Integer.class, leaveId);
            assertThat(reassign).isIn(2, 3);
            assertThat(jdbc.queryForObject("SELECT work_status FROM repair_worker WHERE id=?", Integer.class, worker)).isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM repair_leave_request WHERE id=?", leaveId);
            jdbc.update("DELETE FROM repair_worker WHERE id=?", worker);
            jdbc.update("DELETE FROM sys_user WHERE id=?", workerUser);
        }
    }
}
