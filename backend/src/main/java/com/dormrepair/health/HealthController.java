package com.dormrepair.health;

import com.dormrepair.common.result.Result;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<HealthResponse> health() {
        return Result.success(new HealthResponse("UP", "dorm-repair-backend"));
    }
}
