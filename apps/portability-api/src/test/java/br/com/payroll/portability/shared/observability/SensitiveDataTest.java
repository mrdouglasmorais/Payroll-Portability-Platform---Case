package br.com.payroll.portability.shared.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SensitiveDataTest {

  @Test
  void masksAccountLeavingTail() {
    assertEquals("****99-0", SensitiveData.maskAccount("12399-0"));
  }

  @Test
  void masksAgencyLeavingTail() {
    assertEquals("****34", SensitiveData.maskAgency("1234"));
  }

  @Test
  void nullOrShortBecomesStars() {
    assertEquals("****", SensitiveData.maskAccount(null));
    assertEquals("****", SensitiveData.maskAccount(""));
    assertEquals("****", SensitiveData.maskAccount("12"));
    assertEquals("****", SensitiveData.maskAgency(null));
    assertEquals("****", SensitiveData.maskAgency("1"));
  }
}
