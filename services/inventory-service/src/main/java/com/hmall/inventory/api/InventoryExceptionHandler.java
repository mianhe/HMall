package com.hmall.inventory.api;

import com.hmall.inventory.api.dto.ErrorDto;
import com.hmall.inventory.application.InventoryBadRequestException;
import com.hmall.inventory.application.StockNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = InventoryController.class)
public class InventoryExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryExceptionHandler.class);

    @ExceptionHandler(InventoryBadRequestException.class)
    public ResponseEntity<ErrorDto> handleBadRequest(InventoryBadRequestException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(StockNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .findFirst()
            .orElse("参数校验失败");
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorDto(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleOther(Exception e) {
        log.error("Inventory API 未处理异常", e);
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorDto(message));
    }
}
