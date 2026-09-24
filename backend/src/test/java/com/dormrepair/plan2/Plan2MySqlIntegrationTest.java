package com.dormrepair.plan2;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.domain.entity.SysUser;
import com.dormrepair.domain.mapper.SysUserMapper;
import com.dormrepair.security.jwt.JwtTokenService;
import com.dormrepair.security.model.LoginUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Plan2MySqlIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired SysUserMapper users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService tokens;

    @Test
    void completesFoundationConfigurationChainWithAuthorization() throws Exception {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        String studentName="student_"+suffix;
        String rawPassword="StrongPass123";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
            {"username":"%s","password":"%s","confirmPassword":"%s","realName":"学生甲","phone":"13800138000","roleType":3}
            """.formatted(studentName,rawPassword,rawPassword)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        SysUser student=users.selectByUsername(studentName);
        assertThat(student.getRoleType()).isEqualTo(UserRoleEnum.STUDENT.getCode());
        assertThat(student.getPassword()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword,student.getPassword())).isTrue();
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(studentName,rawPassword)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").isNotEmpty());
        String studentToken=tokens.generate(new LoginUser(student.getId(),student.getUsername(),student.getRealName(),UserRoleEnum.STUDENT));

        mockMvc.perform(post("/api/admin/fault-types").header("Authorization","Bearer "+studentToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"typeCode\":\"DENY\",\"typeName\":\"拒绝\",\"sortNo\":0}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(10003));

        SysUser admin=new SysUser();admin.setUsername("admin_"+suffix);admin.setPassword(passwordEncoder.encode(rawPassword));admin.setRealName("管理员");admin.setRoleType(3);admin.setStatus(1);admin.setDeleted(0);users.insert(admin);
        String adminToken=tokens.generate(new LoginUser(admin.getId(),admin.getUsername(),admin.getRealName(),UserRoleEnum.ADMIN));
        String authorization="Bearer "+adminToken;

        long faultId=id(mockMvc.perform(post("/api/admin/fault-types").header("Authorization",authorization)
            .contentType(MediaType.APPLICATION_JSON).content("{\"typeCode\":\"ELEC_%s\",\"typeName\":\"水电\",\"sortNo\":1}".formatted(suffix)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long campusId=id(mockMvc.perform(post("/api/admin/areas").header("Authorization",authorization)
            .contentType(MediaType.APPLICATION_JSON).content("{\"parentId\":0,\"areaCode\":\"C_%s\",\"areaName\":\"东校区\",\"areaType\":1,\"sortNo\":1}".formatted(suffix)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long areaId=id(mockMvc.perform(post("/api/admin/areas").header("Authorization",authorization)
            .contentType(MediaType.APPLICATION_JSON).content("{\"parentId\":%d,\"areaCode\":\"A_%s\",\"areaName\":\"生活区\",\"areaType\":2,\"sortNo\":1}".formatted(campusId,suffix)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long buildingId=id(mockMvc.perform(post("/api/admin/areas").header("Authorization",authorization)
            .contentType(MediaType.APPLICATION_JSON).content("{\"parentId\":%d,\"areaCode\":\"B_%s\",\"areaName\":\"一号楼\",\"areaType\":3,\"sortNo\":1}".formatted(areaId,suffix)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long workerId=id(mockMvc.perform(post("/api/admin/workers").header("Authorization",authorization)
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"worker_%s\",\"password\":\"%s\",\"realName\":\"维修甲\",\"phone\":\"13900139000\",\"workerNo\":\"W_%s\"}".formatted(suffix,rawPassword,suffix)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        SysUser workerUser=users.selectByUsername("worker_"+suffix);
        String workerToken=tokens.generate(new LoginUser(workerUser.getId(),workerUser.getUsername(),workerUser.getRealName(),UserRoleEnum.WORKER));
        mockMvc.perform(get("/api/admin/workers").header("Authorization","Bearer "+workerToken))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(10003));
        mockMvc.perform(put("/api/admin/workers/{id}/fault-types",workerId).header("Authorization",authorization)
            .contentType(MediaType.APPLICATION_JSON).content("{\"faultTypeIds\":[%d]}".formatted(faultId)))
            .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/workers/{id}/area-scopes",workerId).header("Authorization",authorization)
            .contentType(MediaType.APPLICATION_JSON).content("{\"scopes\":[{\"campusId\":%d,\"areaId\":%d,\"buildingId\":%d}]}".formatted(campusId,areaId,buildingId)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/work-schedules").header("Authorization",authorization)
            .contentType(MediaType.APPLICATION_JSON).content("{\"scheduleName\":\"测试方案\",\"startDate\":\"2035-01-01\",\"endDate\":\"2035-04-30\",\"workStartTime\":\"08:00:00\",\"workEndTime\":\"18:00:00\",\"status\":1}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/fault-types/enabled").header("Authorization","Bearer "+studentToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        mockMvc.perform(get("/api/areas/tree").header("Authorization","Bearer "+studentToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        mockMvc.perform(get("/api/admin/workers/{id}",workerId).header("Authorization",authorization))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.workerNo").value("W_"+suffix));
    }

    private long id(String json) throws Exception {JsonNode root=objectMapper.readTree(json);return root.path("data").path("id").asLong();}
}
