package com.library.seat.common.exception;

import com.library.seat.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public Result<String> handleBadCredentialsException(BadCredentialsException e) {
        return Result.error(401, "用户名或密码错误");
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> handleAccessDeniedException(AccessDeniedException e) {
        return Result.error(403, "权限不足");
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("System Error: ", e);
        return Result.error("系统繁忙，请稍后重试");
    }
}
