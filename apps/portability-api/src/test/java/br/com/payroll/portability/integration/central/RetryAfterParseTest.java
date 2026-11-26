package br.com.payroll.portability.integration.central;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

class RetryAfterParseTest {

  @Test
  void parsesDeltaSeconds() {
    assertEquals(Duration.ofSeconds(12), HttpCentralGateway.parseRetryAfterHeader("12"));
  }

  @Test
  void parsesHttpDateInFuture() {
    String header =
        ZonedDateTime.now(ZoneOffset.UTC)
            .plusSeconds(30)
            .format(DateTimeFormatter.RFC_1123_DATE_TIME);
    Duration delay = HttpCentralGateway.parseRetryAfterHeader(header);
    assertNotNull(delay);
    assertEquals(true, delay.toSeconds() > 0 && delay.toSeconds() <= 300);
  }

  @Test
  void rejectsBlank() {
    assertNull(HttpCentralGateway.parseRetryAfterHeader(" "));
  }
}
