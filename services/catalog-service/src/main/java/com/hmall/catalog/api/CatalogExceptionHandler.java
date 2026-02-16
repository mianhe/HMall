package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.ErrorDto;
import com.hmall.catalog.application.CatalogBadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = { CategoryController.class, ProductController.class, ProductImageController.class, SpecDimensionController.class, SkuController.class, SkuLookupController.class })
public class CatalogExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CatalogExceptionHandler.class);

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

    /** 未分类异常返回 500，便于前端与日志排查 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleOther(Exception e) {
        log.error("Catalog API 未处理异常", e);
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorDto(message));
    }
}
