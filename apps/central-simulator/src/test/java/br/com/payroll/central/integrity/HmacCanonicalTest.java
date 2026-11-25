package br.com.payroll.central.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HmacCanonicalTest {

  @Test
  void knownVectorIsStable() {
    String canonical =
        HmacCanonical.requestString(
            "POST",
            "/autorizacoes",
            "",
            "{\"beneficiarioId\":\"ben-001\",\"convenioId\":\"conv-1\"}".getBytes(),
            "1732564800000",
            "nonce-1",
            "local-test-key",
            "corr-1",
            "");
    HmacProperties props = new HmacProperties();
    props.setHmacSecret("local-test-hmac-secret");
    props.setHmacKeyId("local-test-key");
    HmacService hmac = new HmacService(props);

    String signature = hmac.sign(canonical);
    assertTrue(hmac.verify(canonical, signature));
    assertFalse(hmac.verify(canonical, "00" + signature.substring(2)));
    assertEquals(64, signature.length());
  }

  @Test
  void emptyBodyHashIsDeterministic() {
    assertEquals(
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        HmacCanonical.sha256Hex(new byte[0]));
  }
}
