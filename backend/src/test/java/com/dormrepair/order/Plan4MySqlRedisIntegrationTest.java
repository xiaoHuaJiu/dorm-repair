package com.dormrepair.order;

import com.dormrepair.common.constant.RedisConstant;
import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.security.jwt.JwtTokenService;
import com.dormrepair.security.model.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
class Plan4MySqlRedisIntegrationTest {
 @Autowired JdbcTemplate jdbc; @Autowired MockMvc mvc; @Autowired JwtTokenService tokens; @Autowired StringRedisTemplate redis;
 @Test void duplicateConfirmationCreatesOnceAndSameBizNoReplaysResult() throws Exception {
  String s=UUID.randomUUID().toString().substring(0,8),biz="biz-"+s; long b=8_800_000_000L+Math.abs(s.hashCode())*20L;
  long student=b+1,campus=b+2,area=b+3,building=b+4,room=b+5,fault=b+6,existing=b+7;
  jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,?,1,1,0)",student,"p4_"+s,"x","学生");
  jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)",fault,"P4F_"+s,"水电");
  area(campus,0,"C"+s,"校区",1);area(area,campus,"A"+s,"区域",2);area(building,area,"B"+s,"楼栋",3);area(room,building,"R"+s,"502",4);
  jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,report_time,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,2,NOW(),0)",existing,"OLD"+s,student,"张三","13800138000",campus,area,building,room,fault,"旧故障");
  String auth="Bearer "+tokens.generate(new LoginUser(student,"p4_"+s,"学生",UserRoleEnum.STUDENT));
  String base="{\"bizNo\":\""+biz+"\",\"campusId\":"+campus+",\"areaId\":"+area+",\"buildingId\":"+building+",\"roomId\":"+room+",\"faultTypeId\":"+fault+",\"problemDescription\":\"灯不亮\",\"contactName\":\"张三\",\"contactPhone\":\"13800138000\",\"confirmDuplicate\":";
  mvc.perform(post("/api/student/repair-orders").header("Authorization",auth).contentType("application/json").content(base+"false}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.created").value(false)).andExpect(jsonPath("$.data.duplicate").value(true));
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_idempotent_record WHERE user_id=? AND biz_no=?",Integer.class,student,biz)).isZero();
  String response=mvc.perform(post("/api/student/repair-orders").header("Authorization",auth).contentType("application/json").content(base+"true}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.created").value(true)).andExpect(jsonPath("$.data.duplicate").value(true)).andReturn().getResponse().getContentAsString();
  String replay=mvc.perform(post("/api/student/repair-orders").header("Authorization",auth).contentType("application/json").content(base+"true}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.created").value(true)).andReturn().getResponse().getContentAsString();
  assertThat(replay).isEqualTo(response);
  mvc.perform(post("/api/student/repair-orders").header("Authorization",auth).contentType("application/json").content((base+"true}").replace("灯不亮","空调漏水")))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(17004));
  Long created=jdbc.queryForObject("SELECT id FROM repair_order WHERE student_uid=? AND order_no<>?",Long.class,student,"OLD"+s);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_order_flow WHERE order_id=?",Integer.class,created)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_operation_log WHERE business_type='REPAIR_ORDER' AND business_id=?",Integer.class,created)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_idempotent_record WHERE user_id=? AND biz_no=? AND status=1",Integer.class,student,biz)).isEqualTo(1);
  redis.delete(RedisConstant.repairOrderCreateKey(student,biz));
 }
 private void area(long id,long parent,String code,String name,int type){jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,?,1,0,0)",id,parent,code,name,type);}
}
