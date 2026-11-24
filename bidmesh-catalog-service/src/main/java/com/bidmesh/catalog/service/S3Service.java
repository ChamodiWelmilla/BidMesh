package com.bidmesh.catalog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

@Service
@Slf4j
public class S3Service {

    @Value("${aws.s3.enabled:false}")
    private boolean enabled;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("AWS S3 integration is disabled");
            return;
        }
        if (region.isBlank() || bucketName.isBlank() || accessKey.isBlank() || secretKey.isBlank()) {
            throw new IllegalStateException("AWS S3 configuration is incomplete");
        }

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .httpClientBuilder(software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient.builder())
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String uploadImage(String key, String contentType, InputStream inputStream, long size) {
        if (!enabled || s3Client == null) {
            throw new IllegalStateException("AWS S3 integration is not configured");
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }

    public void deleteImageFromS3(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty() || !imageUrl.contains(".amazonaws.com/")) {
            return;
        }
        if (!enabled || s3Client == null) {
            log.warn("Cannot delete S3 image because AWS S3 integration is disabled: {}", imageUrl);
            return;
        }

        try {
            String key = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            log.info("Deleting image from S3 bucket: {} with key: {}", bucketName, key);
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
                    
            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted image from S3: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete image from S3: {}", e.getMessage());
        }
    }
}
