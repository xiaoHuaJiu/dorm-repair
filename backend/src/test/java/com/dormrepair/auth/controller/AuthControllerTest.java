package com.dormrepair.auth.controller;

import com.dormrepair.auth.service.AuthService;
import com.dormrepair.auth.vo.LoginResponse;
import com.dormrepair.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test void returnsUnifiedLoginResponse() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("token",
            new LoginResponse.UserInfo(1L, "admin", "管理员", 3)));
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.token").value("token"))
            .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test void rejectsBlankCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(10001));
    }
}
