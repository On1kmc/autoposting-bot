package com.ivanov.AutopostingBot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
@Configuration
public class BeansConfig {

    private final String KEY_ID;
    private final String SECRET_KEY;
    private final String REGION;
    private final String S3_ENDPOINT;


    public BeansConfig(@Value("${s3.key}") String keyId, @Value("${s3.secret}") String secretKey,
                       @Value("${s3.region}") String region, @Value("${s3.endpoint}") String s3Endpoint) {
        KEY_ID = keyId;
        SECRET_KEY = secretKey;
        REGION = region;
        S3_ENDPOINT = s3Endpoint;
    }

    @Bean
    public S3Client awsCredentials() {
        AwsCredentials credentials = AwsBasicCredentials.create(KEY_ID, SECRET_KEY);
        return S3Client.builder()
                .httpClient(ApacheHttpClient.create())
                .region(Region.of(REGION))
                .endpointOverride(URI.create(S3_ENDPOINT))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

}
