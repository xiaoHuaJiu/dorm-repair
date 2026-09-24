package com.dormrepair.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RoleAuthorizationIntegrationTest.RoleController.class)
class RoleAuthorizationIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test void anonymousGets401() throws Exception { mockMvc.perform(get("/role/admin")).andExpect(status().isUnauthorized()); }
    @Test @WithMockUser(roles="ADMIN") void adminOnlyCallsAdmin() throws Exception {
        mockMvc.perform(get("/role/admin")).andExpect(status().isOk());
        mockMvc.perform(get("/role/worker")).andExpect(status().isForbidden());
        mockMvc.perform(get("/role/student")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles="WORKER") void workerOnlyCallsWorker() throws Exception {
        mockMvc.perform(get("/role/admin")).andExpect(status().isForbidden());
        mockMvc.perform(get("/role/worker")).andExpect(status().isOk());
        mockMvc.perform(get("/role/student")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles="STUDENT") void studentOnlyCallsStudent() throws Exception {
        mockMvc.perform(get("/role/admin")).andExpect(status().isForbidden());
        mockMvc.perform(get("/role/worker")).andExpect(status().isForbidden());
        mockMvc.perform(get("/role/student")).andExpect(status().isOk());
    }

    @RestController
    static class RoleController {
        @GetMapping("/role/admin") @PreAuthorize("hasRole('ADMIN')") String admin(){return "ok";}
        @GetMapping("/role/worker") @PreAuthorize("hasRole('WORKER')") String worker(){return "ok";}
        @GetMapping("/role/student") @PreAuthorize("hasRole('STUDENT')") String student(){return "ok";}
    }
}
