package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OAuthClient {

  @Value("${oauth.client-id}")
  private String clientId;

  @Value("${oauth.client-secret}")
  private String clientSecret;
}
