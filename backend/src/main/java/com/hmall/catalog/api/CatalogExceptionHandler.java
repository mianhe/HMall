package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.ErrorDto;
import com.hmall.catalog.application.NotLeafCategoryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = { CategoryController.class, ProductController.class })
public class CatalogExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(NotLeafCategoryException.class)
    public ResponseEntity<ErrorDto> handleNotLeaf(NotLeafCategoryException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorDto(e.getMessage()));
    }
}
