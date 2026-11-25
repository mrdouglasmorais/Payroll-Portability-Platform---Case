package br.com.payroll.central.integrity;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class HmacProperties {

  private String hmacSecret;
  private String hmacKeyId = "local-test-key";
  private Duration hmacMaxSkew = Duration.ofMinutes(5);

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

  public Duration getHmacMaxSkew() {
    return hmacMaxSkew;
  }

  public void setHmacMaxSkew(Duration hmacMaxSkew) {
    this.hmacMaxSkew = hmacMaxSkew;
  }
}
