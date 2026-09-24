package com.dormrepair.order;

import com.dormrepair.domain.mapper.RepairOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="spring.sql.init.mode=never")
class Plan6ConcurrencyMySqlTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RepairOrderMapper orders;

    @Test void concurrentAcceptOnlyOneConditionalUpdateSucceeds() throws Exception {
        String s=UUID.randomUUID().toString().substring(0,8); long b=9_700_000_000L+Math.abs(s.hashCode())*20L;
        long student=b+1,user=b+2,worker=b+3,campus=b+4,area=b+5,building=b+6,room=b+7,fault=b+8,order=b+9;
        try {
            jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试',1,1,0)",student,"p6cs_"+s,"x");
            jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试',2,1,0)",user,"p6cw_"+s,"x");
            jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)",worker,user,"P6CW"+s);
            jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)",fault,"P6CF"+s,"并发测试");
            area(campus,0,"P6CC"+s,1); area(area,campus,"P6CA"+s,2); area(building,area,"P6CB"+s,3); area(room,building,"P6CR"+s,4);
            jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,accept_deadline,report_time,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,DATE_ADD(NOW(),INTERVAL 30 MINUTE),NOW(),0)",order,"P6CO"+s,student,"测试","13800138000",campus,area,building,room,fault,"并发",1,worker);
            ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
            Callable<Integer> task=()->{ready.countDown();start.await();LocalDateTime now=LocalDateTime.now();return orders.casAccept(order,worker,now,null,now.plusHours(24));};
            Future<Integer> a=pool.submit(task),c=pool.submit(task); ready.await(5,TimeUnit.SECONDS); start.countDown();
            assertThat(a.get()+c.get()).isEqualTo(1); assertThat(jdbc.queryForObject("SELECT status FROM repair_order WHERE id=?",Integer.class,order)).isEqualTo(2); pool.shutdownNow();
        } finally {
            jdbc.update("DELETE FROM repair_order WHERE id=?",order); jdbc.update("DELETE FROM repair_worker WHERE id=?",worker);
            jdbc.update("DELETE FROM repair_area WHERE id IN (?,?,?,?)",room,building,area,campus); jdbc.update("DELETE FROM repair_fault_type WHERE id=?",fault);
            jdbc.update("DELETE FROM sys_user WHERE id IN (?,?)",student,user);
        }
    }
    private void area(long id,long parent,String code,int type){jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)",id,parent,code,code,type);}
}
