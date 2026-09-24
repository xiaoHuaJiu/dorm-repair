package com.dormrepair.order;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.security.jwt.JwtTokenService;
import com.dormrepair.security.model.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
class Plan3AuthorizationIntegrationTest {
    @Autowired JdbcTemplate jdbc; @Autowired MockMvc mvc; @Autowired JwtTokenService tokens;

    @Test void threeRolesReceiveTheirOwnRowsAndForbiddenDetails() throws Exception {
        String s=UUID.randomUUID().toString().substring(0,8); long b=9_000_000_000L+Math.abs(s.hashCode())*20L;
        long student=b+1,other=b+2,workerUser=b+3,admin=b+4,worker=b+5,campus=b+6,area=b+7,building=b+8,room=b+9,fault=b+10,own=b+11,foreign=b+12;
        user(student,"stu_"+s,1,"学生甲"); user(other,"other_"+s,1,"学生乙"); user(workerUser,"wrk_"+s,2,"维修甲"); user(admin,"adm_"+s,3,"管理员");
        jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,1,0)",worker,workerUser,"W_"+s);
        jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)",fault,"F_"+s,"水电");
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,0,?,?,1,1,0,0)",campus,"C_"+s,"校区");
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,2,1,0,0)",area,campus,"A_"+s,"区域");
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,3,1,0,0)",building,area,"B_"+s,"楼栋");
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,4,1,0,0)",room,building,"R_"+s,"502室");
        order(own,"O1_"+s,student,worker,campus,area,building,room,fault); order(foreign,"O2_"+s,other,null,campus,area,building,room,fault);
        jdbc.update("UPDATE repair_order SET duplicate_order_id=? WHERE id=?",foreign,own);
        String st=bearer(student,"stu_"+s,"学生甲",UserRoleEnum.STUDENT), wk=bearer(workerUser,"wrk_"+s,"维修甲",UserRoleEnum.WORKER), ad=bearer(admin,"adm_"+s,"管理员",UserRoleEnum.ADMIN);
        mvc.perform(get("/api/student/repair-orders").header("Authorization",st)).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/worker/repair-orders").header("Authorization",wk)).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/repair-orders").header("Authorization",ad)).andExpect(status().isOk());
        mvc.perform(get("/api/student/repair-orders/{id}",own).header("Authorization",st)).andExpect(status().isOk()).andExpect(jsonPath("$.data.baseInfo.duplicateOrderId").doesNotExist());
        mvc.perform(get("/api/admin/repair-orders/{id}",own).header("Authorization",ad)).andExpect(status().isOk()).andExpect(jsonPath("$.data.baseInfo.duplicateOrderId").value(foreign));
        mvc.perform(get("/api/student/repair-orders/{id}",foreign).header("Authorization",st)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(17002));
        mvc.perform(get("/api/worker/repair-orders/{id}",foreign).header("Authorization",wk)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(17002));
        mvc.perform(get("/api/admin/repair-orders/{id}",foreign).header("Authorization",ad)).andExpect(status().isOk()).andExpect(jsonPath("$.data.processRecords").isArray()).andExpect(jsonPath("$.data.evaluation").doesNotExist());
    }
    private void user(long id,String username,int role,String name){jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,?,?,1,0)",id,username,"x",name,role);}
    private void order(long id,String no,long student,Long worker,long c,long a,long b,long r,long f){jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,1,?,0)",id,no,student,"联系人","13800138000",c,a,b,r,f,"故障",worker);}
    private String bearer(long id,String username,String name,UserRoleEnum role){return "Bearer "+tokens.generate(new LoginUser(id,username,name,role));}
}
