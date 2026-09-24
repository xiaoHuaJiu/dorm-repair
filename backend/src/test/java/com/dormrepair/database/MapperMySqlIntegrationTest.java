package com.dormrepair.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.sql.init.mode=never")
class MapperMySqlIntegrationTest {
    @Autowired ApplicationContext applicationContext;

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://localhost:" + env("DB_PORT", "3307") + "/" + env("DB_NAME", "dorm_repair") + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");
        registry.add("spring.datasource.username", () -> required("DB_USER"));
        registry.add("spring.datasource.password", () -> required("DB_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Test
    void allMappersCanExecuteSelectByIdAgainstMySql() throws Exception {
        for (String entityName : MapperXmlContractTest.TABLES.values()) {
            Class<?> mapperType = Class.forName("com.dormrepair.domain.mapper." + entityName + "Mapper");
            Object mapper = applicationContext.getBean(mapperType);
            Object result = mapperType.getMethod("selectById", Long.class).invoke(mapper, Long.MIN_VALUE);
            assertThat(result).as(entityName).isNull();
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("缺少测试环境变量：" + name);
        return value;
    }
    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
