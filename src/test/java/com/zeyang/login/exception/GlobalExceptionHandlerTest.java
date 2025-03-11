package com.zeyang.login.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    public void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    // 测试 RuntimeException 处理
    @Test
    public void testHandleRuntimeException() {
        // 准备测试数据
        RuntimeException runtimeException = new RuntimeException("测试运行时异常");

        // 调用异常处理方法
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleRuntimeException(runtimeException);

        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> responseBody = response.getBody();
        assertEquals(1, responseBody.size());
        assertEquals("测试运行时异常", responseBody.get("message"));
    }

    // 测试 MethodArgumentNotValidException 处理
    @Test
    public void testHandleValidationExceptions() {
        // 使用 Mockito 模拟 BindingResult 和 FieldError
        MethodArgumentNotValidException validationException = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // 模拟两个校验错误
        FieldError fieldError1 = new FieldError("user", "username", "用户名不能为空");
        FieldError fieldError2 = new FieldError("user", "password", "密码长度必须大于6位");

        // 配置模拟行为
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // 调用异常处理方法
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(validationException);

        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> responseBody = response.getBody();
        assertEquals(2, responseBody.size());
        assertEquals("用户名不能为空", responseBody.get("username"));
        assertEquals("密码长度必须大于6位", responseBody.get("password"));
    }

    // 测试空错误列表的 MethodArgumentNotValidException
    @Test
    public void testHandleValidationExceptionsWithEmptyErrors() {
        // 模拟空错误列表
        MethodArgumentNotValidException validationException = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of());

        // 调用异常处理方法
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(validationException);

        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> responseBody = response.getBody();
        assertEquals(0, responseBody.size());
    }
}