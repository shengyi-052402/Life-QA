package com.forum.server.handler;

import com.forum.common.exception.BaseException;
import com.forum.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** 全局异常处理器 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** 处理业务异常 */
    @ExceptionHandler(BaseException.class)
    public Result<?> handlerBaseException(BaseException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /** 处理参数校验异常 (JSON请求体) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handlerMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<ObjectError> errors = e.getBindingResult().getAllErrors();
        String message = errors.isEmpty() ? "参数校验失败" : errors.get(0).getDefaultMessage();
        log.error("参数校验异常: {}", message);
        return Result.error(400, message);
    }

    /** 处理参数校验异常 (表单/URL参数) */
    @ExceptionHandler(BindException.class)
    public Result<?> handlerBindException(BindException e) {
        List<ObjectError> errors = e.getBindingResult().getAllErrors();
        String message = errors.isEmpty() ? "参数绑定失败" : errors.get(0).getDefaultMessage();
        log.error("参数绑定异常: {}", message);
        return Result.error(400, message);
    }

    /** 处理系统未知异常 */
    @ExceptionHandler(Exception.class)
    public Result<?> handlerException(Exception e) {
        log.error("系统异常:", e);
        return Result.error(500, "服务器内部错误，请联系管理员");
    }
}
