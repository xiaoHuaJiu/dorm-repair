package com.dormrepair.testsupport;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/errors")
public class TestExceptionController {
    public record ValidBody(@NotBlank(message = "名称不能为空") String name) {}

    @GetMapping("/not-found")
    void notFound() { throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND); }

    @PostMapping("/validation")
    void validation(@Valid @RequestBody ValidBody body) {}

    @GetMapping("/type")
    void type(@RequestParam Long id) {}

    @PostMapping("/post-only")
    void postOnly() {}

    @GetMapping("/system")
    void system() { throw new RuntimeException("jdbc:mysql://secret/path"); }
}
