package com.dormrepair.order;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.order.dto.*;
import com.dormrepair.order.service.StudentRepairOrderCommandService;
import com.dormrepair.security.model.LoginUser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="spring.sql.init.mode=never")
@Transactional
class Plan7MySqlIntegrationTest {
  @Autowired JdbcTemplate jdbc; @Autowired StudentRepairOrderCommandService service;
  @AfterEach void clear(){SecurityContextHolder.clearContext();}

  @Test void secondReworkWarnsWithoutMarkingException(){
    Fixture f=fixture(3,1); login(f.student()); service.rework(f.order(),new CreateReworkRequest("仍未修好",List.of("repair/test/rework.jpg")));
    assertThat(jdbc.queryForMap("SELECT status,rework_count,exception_flag FROM repair_order WHERE id=?",f.order())).containsEntry("status",4).containsEntry("rework_count",2).containsEntry("exception_flag",0);
    assertThat(jdbc.queryForMap("SELECT alert_type,alert_level FROM repair_order_alert WHERE order_id=?",f.order())).containsEntry("alert_type","REWORK_WARNING").containsEntry("alert_level",2);
  }

  @Test void confirmThenEvaluateCompletesStudentClosure(){
    Fixture f=fixture(3,0); login(f.student()); service.confirm(f.order()); service.evaluate(f.order(),new CreateRepairEvaluationRequest(5,"  已修复  "));
    assertThat(jdbc.queryForMap("SELECT status,confirm_time IS NOT NULL confirmed,complete_time IS NOT NULL completed FROM repair_order WHERE id=?",f.order())).containsEntry("status",6).containsEntry("confirmed",1L).containsEntry("completed",1L);
    assertThat(jdbc.queryForMap("SELECT score,content FROM repair_evaluation WHERE order_id=?",f.order())).containsEntry("score",5).containsEntry("content","已修复");
  }

  private Fixture fixture(int status,int count){String s=UUID.randomUUID().toString().substring(0,8);long base=97_000_000_000L+Integer.toUnsignedLong(s.hashCode())*20L;long student=base+1,user=base+2,worker=base+3,order=base+4;jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试学生',1,1,0)",student,"p7s_"+s,"x");jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试维修员',2,1,0)",user,"p7w_"+s,"x");jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)",worker,user,"P7W"+s);jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,repair_submit_time,rework_count,exception_flag,duplicate_flag,deleted) VALUES(?,?,?,'测试','13800138000',1,1,1,1,1,'PLAN7测试',?,?,NOW(),?,0,0,0)",order,"P7O"+s,student,status,worker,count);return new Fixture(student,order);}
  private void login(long student){LoginUser u=new LoginUser(student,"p7student","测试学生",UserRoleEnum.STUDENT);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,List.of()));}
  private record Fixture(long student,long order){}
}
