package com.dormrepair.common.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dormrepair.testsupport.TestExceptionController;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestExceptionController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {
    @Autowired MockMvc mockMvc;

    @Test void mapsBusinessNotFound() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(10004));
    }

    @Test void mapsValidationAndMalformedInputs() throws Exception {
        mockMvc.perform(post("/test/errors/validation").contentType("application/json").content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(10001))
            .andExpect(jsonPath("$.message").value("名称不能为空"));
        mockMvc.perform(get("/test/errors/type").param("id", "abc"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(10001));
        mockMvc.perform(post("/test/errors/validation").contentType("application/json").content("{"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(10001));
    }

    @Test void mapsUnsupportedMethod() throws Exception {
        mockMvc.perform(get("/test/errors/post-only"))
            .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value(10006));
    }

    @Test void hidesUnexpectedExceptionAndLogsThrowable() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            String response = mockMvc.perform(get("/test/errors/system"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(50000))
                .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain("jdbc:mysql://secret/path");
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getThrowableProxy()).isNotNull();
            });
        } finally {
            logger.detachAppender(appender);
        }
    }
}
