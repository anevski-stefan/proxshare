package com.example.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@EnableConfigurationProperties(R2Properties.class)
@TestPropertySource(locations = "classpath:application.properties")
class R2PropertiesTest {

  @Autowired
  R2Properties r2Properties;

  @Test
  void bindsAllProperties() {
    assertThat(r2Properties.accountId()).isEqualTo("test-account");
    assertThat(r2Properties.accessKey()).isEqualTo("test-access-key");
    assertThat(r2Properties.secretKey()).isEqualTo("test-secret-key");
    assertThat(r2Properties.bucketName()).isEqualTo("test-bucket");
    assertThat(r2Properties.publicUrl()).isEqualTo("https://test.example.com");
  }
}
