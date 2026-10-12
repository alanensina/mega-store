package com.alanensina.inventory.services;

import com.alanensina.inventory.exceptions.ImageIsMandatoryException;
import com.alanensina.inventory.exceptions.InvalidImageTypeException;
import com.alanensina.inventory.exceptions.ReadImageErrorException;
import com.alanensina.inventory.exceptions.UploadImageErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Utilities;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductImageStorageServiceTest {

    private S3Client s3Client;
    private ProductImageStorageService service;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        service = new ProductImageStorageService(s3Client, "bucket");
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadShouldStoreSanitizedObjectAndReturnUrl() throws Exception {
        S3Utilities utilities = mock(S3Utilities.class);
        when(s3Client.utilities()).thenReturn(utilities);
        when(utilities.getUrl(any(Consumer.class))).thenReturn(URI.create("https://bucket.s3.amazonaws.com/k").toURL());
        var image = new MockMultipartFile("image", "my photo.PNG", "IMAGE/png", new byte[]{1, 2});

        String url = service.uploadProductImage(image);

        assertEquals("https://bucket.s3.amazonaws.com/k", url);
        var captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals("bucket", captor.getValue().bucket());
        assertTrue(captor.getValue().key().startsWith("products/"));
        assertTrue(captor.getValue().key().endsWith("-my_photo.PNG"));
        var urlConsumer = ArgumentCaptor.forClass(Consumer.class);
        verify(utilities).getUrl(urlConsumer.capture());
        var builder = software.amazon.awssdk.services.s3.model.GetUrlRequest.builder();
        urlConsumer.getValue().accept(builder);
        assertEquals("bucket", builder.build().bucket());
    }

    @Test
    void uploadShouldUseDefaultNameWhenFilenameMissing() throws Exception {
        S3Utilities utilities = mock(S3Utilities.class);
        when(s3Client.utilities()).thenReturn(utilities);
        when(utilities.getUrl(any(Consumer.class))).thenReturn(new URL("https://x/y"));
        MultipartFile image = mock(MultipartFile.class);
        when(image.getOriginalFilename()).thenReturn(null);
        when(image.getContentType()).thenReturn("image/png");
        when(image.getBytes()).thenReturn(new byte[]{1});

        service.uploadProductImage(image);

        var captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertTrue(captor.getValue().key().endsWith("-product-image"));
    }

    @Test
    void uploadShouldRejectNullOrEmptyImage() {
        assertThrows(ImageIsMandatoryException.class, () -> service.uploadProductImage(null));
        assertThrows(ImageIsMandatoryException.class,
                () -> service.uploadProductImage(new MockMultipartFile("image", new byte[0])));
    }

    @Test
    void uploadShouldRejectNonImageOrMissingContentType() {
        assertThrows(InvalidImageTypeException.class, () -> service.uploadProductImage(
                new MockMultipartFile("image", "a.txt", "text/plain", new byte[]{1})));
        MultipartFile noType = mock(MultipartFile.class);
        when(noType.getContentType()).thenReturn(null);
        assertThrows(InvalidImageTypeException.class, () -> service.uploadProductImage(noType));
    }

    @Test
    void uploadShouldTranslateS3Failure() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("boom"));
        var image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1});

        assertThrows(UploadImageErrorException.class, () -> service.uploadProductImage(image));
    }

    @Test
    void uploadShouldTranslateReadFailure() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        when(image.getOriginalFilename()).thenReturn("a.png");
        when(image.getContentType()).thenReturn("image/png");
        when(image.getBytes()).thenThrow(new IOException("io"));

        assertThrows(ReadImageErrorException.class, () -> service.uploadProductImage(image));
    }
}
