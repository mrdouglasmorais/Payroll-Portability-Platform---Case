package br.com.payroll.central.integrity;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NonceReplayGuard {

  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final Duration retention;

  public NonceReplayGuard(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
    this.retention = Duration.ofMinutes(10);
  }

  public boolean registerIfNew(String nonce) {
    Instant now = clock.instant();
    try {
      jdbc.update(
          "INSERT INTO seen_nonces (nonce, seen_at, expires_at) VALUES (?, ?, ?)",
          nonce,
          Timestamp.from(now),
          Timestamp.from(now.plus(retention)));
      jdbc.update("DELETE FROM seen_nonces WHERE expires_at < ?", Timestamp.from(now));
      return true;
    } catch (DuplicateKeyException ex) {
      return false;
    }
  }
}
