package com.alanensina.inventory.configurations;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AwsS3ConfigTest {

    @Test
    void s3ClientIsBuiltForRegion() {
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
        try (S3Client client = new AwsS3Config().s3Client("us-east-1")) {
            assertNotNull(client);
        } finally {
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretAccessKey");
        }
    }
}
