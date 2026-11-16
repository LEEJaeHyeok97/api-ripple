package com.example;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class ServersProperties {

  private List<Server> servers;

  public record Server(String host, int port) {}
}
