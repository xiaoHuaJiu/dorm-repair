package com.dormrepair.order;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.order.dto.*;
import com.dormrepair.order.service.WorkerRepairOrderCommandService;
import com.dormrepair.security.model.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="spring.sql.init.mode=never")
@Transactional
class Plan6MySqlIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WorkerRepairOrderCommandService service;

    @Test void completesWorkerWorkflowAndPersistsAuditTrail() {
        String suffix=UUID.randomUUID().toString().substring(0,8); long base=9_600_000_000L+Math.abs(suffix.hashCode())*20L;
        long student=base+1,user=base+2,worker=base+3,campus=base+4,area=base+5,building=base+6,room=base+7,fault=base+8,order=base+9;
        user(student,"p6s_"+suffix,1); user(user,"p6w_"+suffix,2);
        jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)",worker,user,"P6W"+suffix);
        jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)",fault,"P6F"+suffix,"阶段六故障");
        area(campus,0,"P6C"+suffix,1); area(area,campus,"P6A"+suffix,2); area(building,area,"P6B"+suffix,3); area(room,building,"P6R"+suffix,4);
        jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,accept_deadline,report_time,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,DATE_ADD(NOW(),INTERVAL 30 MINUTE),NOW(),0)",order,"P6O"+suffix,student,"测试","13800138000",campus,area,building,room,fault,"测试",1,worker);
        LoginUser login=new LoginUser(user,"p6w_"+suffix,"测试维修员",UserRoleEnum.WORKER);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));

        service.accept(order,new AcceptRepairOrderRequest(null));
        service.addProcess(order,new AddRepairProcessRequest("现场检查完成",null));
        service.addMaterial(order,new AddMaterialUsageRequest("灯泡","20W",new BigDecimal("1.00"),"个","更换损坏灯泡"));
        service.interrupt(order,new InterruptRepairRequest(1,"等待备用材料"));
        service.resume(order,new ResumeRepairRequest("材料到位，恢复维修"));
        service.submitResult(order,new SubmitRepairResultRequest("已修复并通电测试",null));

        assertThat(jdbc.queryForObject("SELECT status FROM repair_order WHERE id=?",Integer.class,order)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT TIMESTAMPDIFF(HOUR,accept_time,complete_deadline) FROM repair_order WHERE id=?",Integer.class,order)).isEqualTo(24);
        assertThat(jdbc.queryForObject("SELECT repair_submit_time IS NOT NULL AND complete_time IS NULL FROM repair_order WHERE id=?",Boolean.class,order)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_process_record WHERE order_id=?",Integer.class,order)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_material_usage WHERE order_id=?",Integer.class,order)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_order_flow WHERE order_id=?",Integer.class,order)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_operation_log WHERE business_id=? AND business_type='repair_order'",Integer.class,order)).isEqualTo(6);
        SecurityContextHolder.clearContext();
    }
    private void user(long id,String name,int role){jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试',?,1,0)",id,name,"x",role);}
    private void area(long id,long parent,String code,int type){jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)",id,parent,code,code,type);}
}
