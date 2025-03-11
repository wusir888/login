package com.zeyang.login;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@SpringBootTest
@RestController
class LoginApplicationTests {

    @Test
    void testRuntimeException() {
        throw new RuntimeException("测试运行时异常");
    }

    @PostMapping("/test-validation")
    public void testValidation(@Valid @RequestBody User user) {
    }
}

class User {
    @NotNull(message = "用户名不能为空")
    private String username;
    // getters 和 setters
}