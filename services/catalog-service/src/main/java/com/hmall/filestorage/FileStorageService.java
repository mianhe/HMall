package com.hmall.filestorage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class FileStorageService {

    private final MinioClient minioClient;
    private final String bucket;
    private final String endpoint;

    public FileStorageService(
            MinioClient minioClient,
            @Value("${minio.bucket}") String bucket,
            @Value("${minio.endpoint}") String endpoint) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.endpoint = endpoint;
    }

    /**
     * 上传文件到 MinIO，返回可公开访问的 URL。
     * 文件名自动加 UUID 前缀防冲突。
     */
    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString() + extension;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }

        // 返回公开访问 URL：endpoint/bucket/objectName
        return endpoint + "/" + bucket + "/" + objectName;
    }

    /**
     * 从 MinIO 获取对象流。path 格式为 "bucket/objectKey"（与 upload 返回的 URL 后缀一致）。
     */
    public InputStream getObjectStream(String path) {
        if (path == null || !path.contains("/")) {
            throw new IllegalArgumentException("path 须为 bucket/objectKey");
        }
        int firstSlash = path.indexOf('/');
        String bucketName = path.substring(0, firstSlash);
        String objectName = path.substring(firstSlash + 1);
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 若 storedUrl 为本 MinIO 的地址，则返回经后端代理的 URL，供前端同源访问；否则返回原 URL。
     */
    public String toServeUrlIfMinio(String storedUrl) {
        if (storedUrl == null) return null;
        String prefix = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        if (!storedUrl.startsWith(prefix)) return storedUrl;
        String path = storedUrl.substring(prefix.length()).replaceFirst("^/", "");
        return "/api/files/serve?path=" + java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
