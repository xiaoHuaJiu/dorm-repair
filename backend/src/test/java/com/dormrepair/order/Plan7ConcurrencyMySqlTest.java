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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="spring.sql.init.mode=never")
class Plan7ConcurrencyMySqlTest {
  @Autowired JdbcTemplate jdbc; @Autowired StudentRepairOrderCommandService service;
  private final List<Fixture> fixtures=new ArrayList<>();
  @AfterEach void cleanup(){for(Fixture f:fixtures){jdbc.update("DELETE FROM sys_operation_log WHERE business_id=? AND business_type='repair_order'",f.order());jdbc.update("DELETE FROM repair_order_flow WHERE order_id=?",f.order());jdbc.update("DELETE FROM repair_order_alert WHERE order_id=?",f.order());jdbc.update("DELETE FROM repair_rework_record WHERE order_id=?",f.order());jdbc.update("DELETE FROM repair_evaluation WHERE order_id=?",f.order());jdbc.update("DELETE FROM repair_order WHERE id=?",f.order());jdbc.update("DELETE FROM repair_worker WHERE id=?",f.worker());jdbc.update("DELETE FROM sys_user WHERE id IN (?,?)",f.student(),f.workerUser());}}

  @Test void confirmAndReworkRaceAllowsOnlyOneTransition() throws Exception {
    Fixture f=fixture(3);AtomicInteger success=new AtomicInteger();runTogether(()->{login(f.student());service.confirm(f.order());success.incrementAndGet();},()->{login(f.student());service.rework(f.order(),new CreateReworkRequest("并发返工",null));success.incrementAndGet();});
    assertThat(success.get()).isEqualTo(1);int status=jdbc.queryForObject("SELECT status FROM repair_order WHERE id=?",Integer.class,f.order());int records=jdbc.queryForObject("SELECT COUNT(*) FROM repair_rework_record WHERE order_id=?",Integer.class,f.order());assertThat(status==6&&records==0||status==4&&records==1).isTrue();
  }

  @Test void concurrentEvaluationPersistsExactlyOne() throws Exception {
    Fixture f=fixture(6);AtomicInteger success=new AtomicInteger();runTogether(()->{login(f.student());service.evaluate(f.order(),new CreateRepairEvaluationRequest(5,"评价A"));success.incrementAndGet();},()->{login(f.student());service.evaluate(f.order(),new CreateRepairEvaluationRequest(4,"评价B"));success.incrementAndGet();});
    assertThat(success.get()).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_evaluation WHERE order_id=?",Integer.class,f.order())).isEqualTo(1);
  }

  private void runTogether(Throwing first,Throwing second)throws Exception{ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);List<Future<?>> futures=List.of(pool.submit(()->run(start,first)),pool.submit(()->run(start,second)));start.countDown();for(Future<?> f:futures){try{f.get(10,TimeUnit.SECONDS);}catch(ExecutionException ignored){}}pool.shutdownNow();}
  private void run(CountDownLatch start,Throwing work){try{start.await();work.run();}catch(Exception ignored){}finally{SecurityContextHolder.clearContext();}}
  private void login(long student){LoginUser u=new LoginUser(student,"p7student","测试学生",UserRoleEnum.STUDENT);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,List.of()));}
  private Fixture fixture(int status){String s=UUID.randomUUID().toString().substring(0,8);long base=98_000_000_000L+Integer.toUnsignedLong(s.hashCode())*20L;Fixture f=new Fixture(base+1,base+2,base+3,base+4);jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试学生',1,1,0)",f.student(),"p7cs_"+s,"x");jdbc.update("INSERT INTO sys_user(id,username,password,real_name,role_type,status,deleted) VALUES(?,?,?,'测试维修员',2,1,0)",f.workerUser(),"p7cw_"+s,"x");jdbc.update("INSERT INTO repair_worker(id,user_id,worker_no,work_status,deleted) VALUES(?,?,?,0,0)",f.worker(),f.workerUser(),"P7CW"+s);jdbc.update("INSERT INTO repair_order(id,order_no,student_uid,contact_name,contact_phone,campus_id,area_id,building_id,room_id,fault_type_id,problem_description,status,current_assignee_id,repair_submit_time,rework_count,exception_flag,duplicate_flag,deleted) VALUES(?,?,?,'测试','13800138000',1,1,1,1,1,'PLAN7并发测试',?,?,NOW(),0,0,0,0)",f.order(),"P7CO"+s,f.student(),status,f.worker());fixtures.add(f);return f;}
  @FunctionalInterface interface Throwing{void run()throws Exception;}
  private record Fixture(long student,long workerUser,long worker,long order){}
}
