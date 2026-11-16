package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class StorageConfig {

  @Bean
  @Profile("prod")
  public S3Store s3Store(@Value("${storage.s3.bucket}") String bucket) {
    return new S3Store(bucket);
  }

  @Bean
  public LocalStore localStore(@Value("${storage.local.path:/tmp/store}") String path) {
    return new LocalStore(path);
  }

  public record S3Store(String bucket) {}

  public record LocalStore(String path) {}
}
