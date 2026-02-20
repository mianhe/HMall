package com.hmall.order.api;

import com.hmall.order.api.dto.ErrorDto;
import com.hmall.order.application.OrderBadRequestException;
import com.hmall.order.infrastructure.inventory.InventoryUnavailableException;
import com.hmall.order.infrastructure.payment.PaymentCallException;
import com.hmall.order.infrastructure.payment.PaymentUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = { OrderController.class })
public class OrderExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(OrderBadRequestException.class)
    public ResponseEntity<ErrorDto> handleBadRequest(OrderBadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    public ResponseEntity<ErrorDto> handleInventoryUnavailable(InventoryUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(PaymentUnavailableException.class)
    public ResponseEntity<ErrorDto> handlePaymentUnavailable(PaymentUnavailableException e) {
        String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "支付服务暂时不可用，请稍后重试";
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorDto(msg));
    }

    @ExceptionHandler(PaymentCallException.class)
    public ResponseEntity<ErrorDto> handlePaymentCall(PaymentCallException e) {
        String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "创建支付失败";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .findFirst()
            .orElse("参数校验失败");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(message));
    }
}
