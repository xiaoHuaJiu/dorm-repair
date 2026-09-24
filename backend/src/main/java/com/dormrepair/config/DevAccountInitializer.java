package com.dormrepair.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.dev-seed", name = "enabled", havingValue = "true")
public class DevAccountInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder encoder;
    private final DevAccountProperties properties;
    public DevAccountInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder encoder, DevAccountProperties properties) {
        this.jdbcTemplate = jdbcTemplate; this.encoder = encoder; this.properties = properties;
    }
    @Override public void run(ApplicationArguments args) {
        insert(properties.getAdmin(), "本地管理员", 3, 1);
        insert(properties.getWorker(), "本地维修人员", 2, 1);
        insert(properties.getStudent(), "本地学生", 1, 1);
        insert(properties.getDisabled(), "本地停用账号", 1, 0);
    }
    private void insert(DevAccountProperties.Account account, String realName, int role, int status) {
        if (account.getUsername() == null || account.getUsername().isBlank()
            || account.getPassword() == null || account.getPassword().isBlank()) {
            throw new IllegalStateException("启用开发账号初始化时必须提供全部本机账号凭据");
        }
        jdbcTemplate.update("""
            INSERT IGNORE INTO sys_user(username,password,real_name,role_type,status,create_time,update_time,deleted)
            VALUES (?,?,?,?,?,NOW(),NOW(),0)
            """, account.getUsername(), encoder.encode(account.getPassword()), realName, role, status);
    }
}
