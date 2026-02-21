package com.hmall.fulfillment.api;

import com.hmall.fulfillment.api.dto.ErrorDto;
import com.hmall.fulfillment.application.FulfillmentBadRequestException;
import com.hmall.fulfillment.application.FulfillmentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = FulfillmentController.class)
public class FulfillmentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentExceptionHandler.class);

    @ExceptionHandler(FulfillmentBadRequestException.class)
    public ResponseEntity<ErrorDto> handleBadRequest(FulfillmentBadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(FulfillmentNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(FulfillmentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .findFirst()
            .orElse("参数校验失败");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleOther(Exception e) {
        log.error("Fulfillment API 未处理异常", e);
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto(message));
    }
}
