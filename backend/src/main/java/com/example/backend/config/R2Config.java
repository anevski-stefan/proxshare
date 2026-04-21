package com.example.backend.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class R2Config {

  private final R2Properties r2Properties;

  public R2Config(R2Properties r2Properties) {
    this.r2Properties = r2Properties;
  }

  @Bean
  public S3Client s3Client() {
    var credentials = AwsBasicCredentials.create(
        r2Properties.accessKey(),
        r2Properties.secretKey()
    );

    return S3Client.builder()
        .endpointOverride(
            URI.create("https://" + r2Properties.accountId() + ".r2.cloudflarestorage.com"))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .region(Region.of("auto"))
        .build();
  }
}
