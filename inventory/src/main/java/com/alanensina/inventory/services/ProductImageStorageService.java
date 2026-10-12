package com.alanensina.inventory.services;

import com.alanensina.inventory.exceptions.ImageIsMandatoryException;
import com.alanensina.inventory.exceptions.InvalidImageTypeException;
import com.alanensina.inventory.exceptions.ReadImageErrorException;
import com.alanensina.inventory.exceptions.UploadImageErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class ProductImageStorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public ProductImageStorageService(S3Client s3Client, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public String uploadProductImage(MultipartFile image) {
        validateImage(image);
        var originalName = image.getOriginalFilename() == null ? "product-image" : image.getOriginalFilename();
        var objectKey = "products/%s-%s".formatted(UUID.randomUUID(), sanitizeFileName(originalName));

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(image.getContentType())
                            .build(),
                    RequestBody.fromBytes(image.getBytes())
            );

            var utilities = s3Client.utilities();
            return utilities.getUrl(builder -> builder.bucket(bucketName).key(objectKey)).toExternalForm();
        } catch (SdkException e) {
            log.error("Could not upload product image to S3 bucket '{}': {}", bucketName, e.getMessage());
            throw new UploadImageErrorException();
        } catch (IOException e) {
            log.error("Could not read uploaded product image: {}", e.getMessage());
            throw new ReadImageErrorException();
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ImageIsMandatoryException();
        }
        var contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new InvalidImageTypeException();
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
