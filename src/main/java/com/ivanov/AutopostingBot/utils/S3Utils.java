package com.ivanov.AutopostingBot.utils;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileInputStream;

@Component
public class S3Utils {

    private final String bucket;
    private final String s3Endpoint;
    private final S3Client s3Client;
    public S3Utils(@Value("${s3.bucket}") String bucket,
                   @Value("${s3.endpoint}") String s3Endpoint,
                   S3Client s3Client) {
        this.bucket = bucket;
        this.s3Endpoint = s3Endpoint;
        this.s3Client = s3Client;
    }


    @SneakyThrows
    public String uploadImage(File file) {

        FileInputStream in = new FileInputStream(file);
        byte[] bytes = in.readAllBytes();
        in.close();

        String key = file.getName();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/jpeg")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
        return s3Endpoint + "/" + bucket + "/" + key;
    }
}
