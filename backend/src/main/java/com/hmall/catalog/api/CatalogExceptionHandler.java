package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.ErrorDto;
import com.hmall.catalog.application.CatalogBadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = { CategoryController.class, ProductController.class, SpecDimensionController.class, SkuController.class })
public class CatalogExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(CatalogBadRequestException.class)
    public ResponseEntity<ErrorDto> handleBadRequest(CatalogBadRequestException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorDto(e.getMessage()));
    }
}
