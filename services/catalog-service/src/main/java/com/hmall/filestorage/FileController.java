package com.hmall.filestorage;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 上传文件，返回可访问的 URL。
     * POST /api/files/upload
     * Content-Type: multipart/form-data
     * 参数: file (文件)
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.upload(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * 代理 MinIO 中的文件，供前端同源加载图片等，避免直连 MinIO 的 CORS/鉴权问题。
     * GET /api/files/serve?path=bucket/objectKey
     */
    @GetMapping("/serve")
    public ResponseEntity<Resource> serve(@RequestParam("path") String path) {
        Resource resource = new InputStreamResource(fileStorageService.getObjectStream(path));
        String contentType = contentTypeFromPath(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(resource);
    }

    private static String contentTypeFromPath(String path) {
        if (path == null) return "application/octet-stream";
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
