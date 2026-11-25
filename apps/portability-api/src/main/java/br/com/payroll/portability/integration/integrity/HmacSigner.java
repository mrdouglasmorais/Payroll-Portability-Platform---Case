package br.com.payroll.portability.integration.integrity;

import br.com.payroll.portability.integration.central.CentralProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacSigner {

  private final byte[] secret;
  private final String keyId;

  public HmacSigner(CentralProperties properties) {
    this.secret =
        Objects.requireNonNull(properties.getHmacSecret(), "app.central.hmac-secret")
            .getBytes(StandardCharsets.UTF_8);
    this.keyId = Objects.requireNonNull(properties.getHmacKeyId(), "app.central.hmac-key-id");
  }

  public String keyId() {
    return keyId;
  }

  public String sign(String canonical) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(raw);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("HMAC signing failed", e);
    }
  }

  public boolean verify(String canonical, String providedSignature) {
    if (providedSignature == null || providedSignature.isBlank()) {
      return false;
    }
    String expected = sign(canonical);
    return constantTimeEquals(expected, providedSignature.trim().toLowerCase());
  }

  private static boolean constantTimeEquals(String a, String b) {
    if (a.length() != b.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
      result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
  }
}
