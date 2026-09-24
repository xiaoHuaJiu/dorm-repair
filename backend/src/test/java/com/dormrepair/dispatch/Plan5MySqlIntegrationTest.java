package com.dormrepair.dispatch;

import com.dormrepair.dispatch.model.*; import com.dormrepair.dispatch.service.RepairDispatchService;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime; import java.util.Set; import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="spring.sql.init.mode=never") @Transactional
class Plan5MySqlIntegrationTest {
 @Autowired JdbcTemplate jdbc; @Autowired RepairDispatchService dispatch;
 @Test void dispatchesBySkillAreaAvailabilityWorkloadAndCas(){
  String s=UUID.randomUUID().toString().substring(0,8);long b=9_100_000_000L+Math.abs(s.hashCode())*30L;
  long student=b+1,userA=b+2,userB=b+3,workerA=b+4,workerB=b+5,campus=b+6,area=b+7,building=b+8,room=b+9,fault=b+10,order=b+11,loadOrder=b+12;
  user(student,"s5s_"+s,1);user(userA,"s5a_"+s,2);user(userB,"s5b_"+s,2);
  jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)",workerA,userA,"S5A"+s);
  jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)",workerB,userB,"S5B"+s);
  jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)",fault,"S5F"+s,"阶段五故障");
  area(campus,0,"S5C"+s,1);area(area,campus,"S5A"+s,2);area(building,area,"S5B"+s,3);area(room,building,"S5R"+s,4);
  for(long w:new long[]{workerA,workerB}){jdbc.update("INSERT INTO repair_worker_fault_type(worker_id,fault_type_id,status) VALUES(?,?,1)",w,fault);jdbc.update("INSERT INTO repair_worker_area_scope(worker_id,campus_id,area_id,building_id,status) VALUES(?,?,?,?,1)",w,campus,area,building);}
  jdbc.update("INSERT INTO repair_work_schedule(schedule_name,start_date,end_date,work_start_time,work_end_time,status) VALUES(?,CURRENT_DATE,CURRENT_DATE,?,?,1)","S5"+s,"00:00:00","23:59:59");
  order(loadOrder,"S5L"+s,student,campus,area,building,room,fault,1,workerA);order(order,"S5O"+s,student,campus,area,building,room,fault,0,null);
  DispatchResult result=dispatch.dispatch(order,Set.of(),DispatchSourceType.INITIAL_REPORT);
  assertThat(result.success()).isTrue();assertThat(result.workerId()).isEqualTo(workerB);
  assertThat(jdbc.queryForObject("SELECT status FROM repair_order WHERE id=?",Integer.class,order)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT current_assignee_id FROM repair_order WHERE id=?",Long.class,order)).isEqualTo(workerB);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_order_flow WHERE order_id=? AND operation_type=2 AND source_type=1",Integer.class,order)).isEqualTo(1);
  assertThat(jdbc.update("UPDATE repair_order SET current_assignee_id=? WHERE id=? AND status=0 AND current_assignee_id IS NULL",workerA,order)).isZero();
 }
 private void user(long id,String name,int role){jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试',?,1,0)",id,name,"x",role);}
 private void area(long id,long parent,String code,int type){jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)",id,parent,code,code,type);}
 private void order(long id,String no,long student,long campus,long area,long building,long room,long fault,int status,Long worker){jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,report_time,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),0)",id,no,student,"测试","13800138000",campus,area,building,room,fault,"测试",status,worker);}
}
