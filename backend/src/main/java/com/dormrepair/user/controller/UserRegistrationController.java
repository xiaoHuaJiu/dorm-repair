package com.dormrepair.user.controller;

import com.dormrepair.common.result.Result;
import com.dormrepair.user.dto.StudentRegisterRequest;
import com.dormrepair.user.service.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserRegistrationController {
    private final UserRegistrationService registrationService;

    public UserRegistrationController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody StudentRegisterRequest request) {
        registrationService.register(request);
        return Result.success();
    }
}
