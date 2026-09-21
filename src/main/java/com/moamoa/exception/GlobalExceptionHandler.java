package com.moamoa.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AppException.class)
  public Object app(AppException e, HttpServletRequest r) {
    return response(e.getStatus(), e.getMessage(), r);
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  public Object validation(Exception e, HttpServletRequest r) {
    return response(HttpStatus.BAD_REQUEST, "입력값을 확인해주세요. 비밀번호는 10~64자, 닉네임은 1~40자입니다.", r);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public Object conflict(Exception e, HttpServletRequest r) {
    return response(HttpStatus.CONFLICT, "이미 등록된 항목입니다. 입력값을 확인해주세요.", r);
  }

  private Object response(HttpStatus status, String message, HttpServletRequest request) {
    if (request.getRequestURI().startsWith("/api/"))
      return ResponseEntity.status(status).body(Map.of("message", message));
    var view = new ModelAndView("error");
    view.setStatus(status);
    view.addObject("message", message);
    return view;
  }
}
