package com.dormrepair.order;

import com.dormrepair.domain.mapper.RepairOrderMapper;
import com.dormrepair.order.dto.RepairOrderQueryRequest;
import com.dormrepair.order.model.RepairOrderQueryCondition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="spring.sql.init.mode=never")
@Transactional
class RepairOrderMapperMySqlTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RepairOrderMapper mapper;

    @Test void filtersByOwnershipRoomFlagsAndTimeoutWithoutDuplicateRows(){
        String s=UUID.randomUUID().toString().substring(0,8); long base=8_000_000_000L+Math.abs(s.hashCode())*10L;
        long student=base+1, workerUser=base+2, worker=base+3, campus=base+4, area=base+5, building=base+6, room=base+7, fault=base+8, order=base+9;
        jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,?,1,1,0)",student,"stu_"+s,"x","学生");
        jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,?,2,1,0)",workerUser,"wrk_"+s,"x","维修员");
        jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,1,0)",worker,workerUser,"W_"+s);
        jdbc.update("INSERT INTO repair_fault_type(id,type_code,type_name,status,sort_no) VALUES(?,?,?,1,0)",fault,"F_"+s,"水电");
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,0,?,?,1,1,0,0)",campus,"C_"+s,"校区");
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,2,1,0,0)",area,campus,"A_"+s,"区域");
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,3,1,0,0)",building,area,"B_"+s,"楼栋");
        jdbc.update("INSERT INTO repair_area(id,parent_id,area_code,area_name,area_type,status,sort_no,deleted) VALUES(?,?,?,?,4,1,0,0)",room,building,"R_"+s,"502室");
        jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,accept_deadline,exception_flag,duplicate_flag,deleted) VALUES(?,?,?,?,?,?,?,?,?,?,?,1,?,?,1,1,0)",order,"O_"+s,student,"学生","13800138000",campus,area,building,room,fault,"漏水",worker,LocalDateTime.now().minusMinutes(1));

        RepairOrderQueryRequest request=new RepairOrderQueryRequest(); request.setRoomId(room); request.setExceptionFlag(true); request.setSuspectedDuplicate(true); request.setAcceptTimeout(true);
        RepairOrderQueryCondition condition=new RepairOrderQueryCondition(request); condition.setStudentUid(student); condition.setNow(LocalDateTime.now());
        var rows=mapper.selectPage(condition);
        assertThat(rows).singleElement().satisfies(row->{assertThat(row.getOrderId()).isEqualTo(order);assertThat(row.getRoomName()).isEqualTo("502室");});
    }
}
