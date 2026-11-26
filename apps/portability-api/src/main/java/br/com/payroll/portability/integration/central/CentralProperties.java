package br.com.payroll.portability.integration.central;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.central")
public class CentralProperties {

  private String baseUrl = "http://localhost:8081";
  private String hmacSecret = "local-test-hmac-secret";
  private String hmacKeyId = "local-test-key";
  private Duration connectTimeout = Duration.ofSeconds(2);
  private Duration readTimeout = Duration.ofSeconds(5);
  private int maxAttempts = 8;
  private Duration pollDelay = Duration.ofSeconds(1);
  private Duration leaseDuration = Duration.ofSeconds(30);
  private boolean workerEnabled = true;
  private Duration workerTick = Duration.ofSeconds(1);
  private Duration maxBackoff = Duration.ofSeconds(60);
  private Duration maxPollDuration = Duration.ofHours(24);

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getHmacSecret() {
    return hmacSecret;
  }

  public void setHmacSecret(String hmacSecret) {
    this.hmacSecret = hmacSecret;
  }

  public String getHmacKeyId() {
    return hmacKeyId;
  }

  public void setHmacKeyId(String hmacKeyId) {
    this.hmacKeyId = hmacKeyId;
  }

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public Duration getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(Duration readTimeout) {
    this.readTimeout = readTimeout;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public Duration getPollDelay() {
    return pollDelay;
  }

  public void setPollDelay(Duration pollDelay) {
    this.pollDelay = pollDelay;
  }

  public Duration getLeaseDuration() {
    return leaseDuration;
  }

  public void setLeaseDuration(Duration leaseDuration) {
    this.leaseDuration = leaseDuration;
  }

  public boolean isWorkerEnabled() {
    return workerEnabled;
  }

  public void setWorkerEnabled(boolean workerEnabled) {
    this.workerEnabled = workerEnabled;
  }

  public Duration getWorkerTick() {
    return workerTick;
  }

  public void setWorkerTick(Duration workerTick) {
    this.workerTick = workerTick;
  }

  public Duration getMaxBackoff() {
    return maxBackoff;
  }

  public void setMaxBackoff(Duration maxBackoff) {
    this.maxBackoff = maxBackoff;
  }

  public Duration getMaxPollDuration() {
    return maxPollDuration;
  }

  public void setMaxPollDuration(Duration maxPollDuration) {
    this.maxPollDuration = maxPollDuration;
  }
}
