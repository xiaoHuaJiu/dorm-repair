package com.dormrepair.security.jwt;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.security.model.LoginUser;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class JwtTokenServiceTest {
    private static final String SECRET = "test-only-secret-key-with-at-least-32-bytes";

    @Test void generatesAndParsesLoginUser() {
        JwtTokenService service = service(Duration.ofHours(1));
        String token = service.generate(new LoginUser(8L, "worker", "维修员", UserRoleEnum.WORKER));
        LoginUser parsed = service.parse(token);
        assertThat(parsed.getUserId()).isEqualTo(8L);
        assertThat(parsed.getUsername()).isEqualTo("worker");
        assertThat(parsed.getRoleType()).isEqualTo(UserRoleEnum.WORKER);
    }

    @Test void rejectsTamperedAndExpiredTokens() throws Exception {
        JwtTokenService normal = service(Duration.ofHours(1));
        String token = normal.generate(new LoginUser(1L, "student", "学生", UserRoleEnum.STUDENT));
        assertThatThrownBy(() -> normal.parse(token + "x")).isInstanceOf(JwtTokenException.class);
        JwtTokenService expired = service(Duration.ofMillis(1));
        String expiredToken = expired.generate(new LoginUser(1L, "student", "学生", UserRoleEnum.STUDENT));
        Thread.sleep(5);
        assertThatThrownBy(() -> expired.parse(expiredToken)).isInstanceOf(JwtTokenException.class);
    }

    private JwtTokenService service(Duration ttl) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET); properties.setTtl(ttl);
        return new JwtTokenService(properties);
    }
}
