package com.dormrepair.reminder;

import com.dormrepair.reminder.enums.ReminderLevel;
import com.dormrepair.reminder.service.RepairReminderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties={"spring.sql.init.mode=never","app.task.enabled=false"})
@Transactional
class Plan10MySqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RepairReminderService service;

    @Test void repeatedAcceptReminderScanCreatesOneRecord() {
        Fixture f=fixture(1); LocalDateTime scan=LocalDateTime.now().withNano(0); LocalDateTime deadline=scan.plusMinutes(10);
        jdbc.update("UPDATE repair_order SET accept_deadline=? WHERE id=?",deadline,f.order());
        service.createAcceptReminders(scan, ReminderLevel.MINUS_10); service.createAcceptReminders(scan, ReminderLevel.MINUS_10);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_reminder_record WHERE order_id=? AND reminder_type=1 AND reminder_level=1 AND receiver_id=?",Integer.class,f.order(),f.workerUserA())).isEqualTo(1);
    }

    @Test void completeTimeoutRemindsButKeepsRepairingStatus() {
        Fixture f=fixture(2); LocalDateTime scan=LocalDateTime.now().withNano(0); LocalDateTime deadline=scan.minusMinutes(1);
        jdbc.update("UPDATE repair_order SET complete_deadline=? WHERE id=?",deadline,f.order());
        service.createCompleteTimeoutReminders(scan); service.createCompleteTimeoutReminders(scan);
        assertThat(jdbc.queryForObject("SELECT status FROM repair_order WHERE id=?",Integer.class,f.order())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_reminder_record WHERE order_id=? AND reminder_type=2 AND reminder_level=3 AND receiver_id=?",Integer.class,f.order(),f.workerUserA())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_reminder_record WHERE order_id=? AND reminder_type=2 AND reminder_level=3 AND receiver_id=?",Integer.class,f.order(),f.admin())).isEqualTo(1);
    }

    @Test void acceptTimeoutReassignsAndExcludesOldWorker() {
        Fixture f=fixture(1); LocalDateTime scan=LocalDateTime.now().withNano(0);
        jdbc.update("UPDATE repair_order SET accept_deadline=? WHERE id=?",scan.minusMinutes(1),f.order());
        service.processAcceptTimeouts(scan);
        assertThat(jdbc.queryForMap("SELECT status,current_assignee_id,accept_deadline>NOW() future_deadline FROM repair_order WHERE id=?",f.order()))
            .containsEntry("status",1).containsEntry("current_assignee_id",f.workerB()).containsEntry("future_deadline",1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_order_flow WHERE order_id=? AND source_type=2",Integer.class,f.order())).isEqualTo(1);
    }

    private Fixture fixture(int status){String s=UUID.randomUUID().toString().substring(0,8);long b=101_000_000_000L+Integer.toUnsignedLong(s.hashCode())*30L;long student=b+1,userA=b+2,userB=b+3,admin=b+4,workerA=b+5,workerB=b+6,campus=b+7,area=b+8,building=b+9,room=b+10,fault=b+11,order=b+12;
        user(student,"p10s_"+s,1);user(userA,"p10a_"+s,2);user(userB,"p10b_"+s,2);user(admin,"p10m_"+s,3);
        jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)",workerA,userA,"P10A"+s);jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)",workerB,userB,"P10B"+s);
        jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)",fault,"P10F"+s,"阶段十故障");area(campus,0,"P10C"+s,1);area(area,campus,"P10A"+s,2);area(building,area,"P10B"+s,3);area(room,building,"P10R"+s,4);
        for(long w:new long[]{workerA,workerB}){jdbc.update("INSERT INTO repair_worker_fault_type(worker_id,fault_type_id,status) VALUES(?,?,1)",w,fault);jdbc.update("INSERT INTO repair_worker_area_scope(worker_id,campus_id,area_id,building_id,status) VALUES(?,?,?,?,1)",w,campus,area,building);}
        jdbc.update("INSERT INTO repair_work_schedule(schedule_name,start_date,end_date,work_start_time,work_end_time,status) VALUES(?,CURRENT_DATE,CURRENT_DATE,?,?,1)","P10"+s,"00:00:00","23:59:59");
        jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,report_time,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),0)",order,"P10O"+s,student,"测试","13800138000",campus,area,building,room,fault,"PLAN10测试",status,workerA);return new Fixture(userA,admin,workerA,workerB,order);}
    private void user(long id,String name,int role){jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试',?,1,0)",id,name,"x",role);} private void area(long id,long parent,String code,int type){jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)",id,parent,code,code,type);}
    private record Fixture(long workerUserA,long admin,long workerA,long workerB,long order){}
}
