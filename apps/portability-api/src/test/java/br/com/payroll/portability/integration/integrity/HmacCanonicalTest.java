package br.com.payroll.portability.integration.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class HmacCanonicalTest {

  @Test
  void requestCanonicalIsStable() {
    String a =
        HmacCanonical.requestString(
            "post",
            "/operacoes-portabilidade",
            "",
            HmacCanonical.utf8("{\"a\":1}"),
            "1",
            "n1",
            "k1",
            "c1",
            "idem-1");
    String b =
        HmacCanonical.requestString(
            "POST",
            "/operacoes-portabilidade",
            "",
            HmacCanonical.utf8("{\"a\":1}"),
            "1",
            "n1",
            "k1",
            "c1",
            "idem-1");
    assertEquals(a, b);
  }

  @Test
  void bodyTamperChangesHash() {
    String a =
        HmacCanonical.requestString(
            "POST", "/x", "", HmacCanonical.utf8("a"), "1", "n", "k", "c", "");
    String b =
        HmacCanonical.requestString(
            "POST", "/x", "", HmacCanonical.utf8("b"), "1", "n", "k", "c", "");
    assertNotEquals(a, b);
  }
}
