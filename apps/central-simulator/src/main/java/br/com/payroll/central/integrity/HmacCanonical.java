package br.com.payroll.central.integrity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class HmacCanonical {

  private HmacCanonical() {}

  public static String sha256Hex(byte[] body) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(body == null ? new byte[0] : body));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public static String requestString(
      String method,
      String path,
      String query,
      byte[] body,
      String timestamp,
      String nonce,
      String keyId,
      String correlationId,
      String idempotencyKey) {
    return String.join(
        "\n",
        Objects.requireNonNull(method).toUpperCase(),
        nullToEmpty(path),
        nullToEmpty(query),
        sha256Hex(body),
        nullToEmpty(timestamp),
        nullToEmpty(nonce),
        nullToEmpty(keyId),
        nullToEmpty(correlationId),
        nullToEmpty(idempotencyKey));
  }

  public static String responseString(
      String method,
      String path,
      String query,
      byte[] body,
      String timestamp,
      String nonce,
      String keyId,
      String correlationId,
      String requestNonce,
      int statusCode) {
    return String.join(
        "\n",
        Objects.requireNonNull(method).toUpperCase(),
        nullToEmpty(path),
        nullToEmpty(query),
        sha256Hex(body),
        nullToEmpty(timestamp),
        nullToEmpty(nonce),
        nullToEmpty(keyId),
        nullToEmpty(correlationId),
        nullToEmpty(requestNonce),
        Integer.toString(statusCode));
  }

  public static byte[] utf8(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
