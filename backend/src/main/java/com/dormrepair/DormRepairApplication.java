package com.dormrepair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.dormrepair.security.jwt.JwtProperties;
import com.dormrepair.config.DevAccountProperties;
import com.dormrepair.config.DispatchProperties;
import com.dormrepair.config.LeaveProperties;

@EnableScheduling
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({JwtProperties.class, DevAccountProperties.class, DispatchProperties.class, LeaveProperties.class})
public class DormRepairApplication {

    public static void main(String[] args) {
        SpringApplication.run(DormRepairApplication.class, args);
    }
}
