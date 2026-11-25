package br.com.payroll.portability.portability.adapter.persistence;

import br.com.payroll.portability.portability.application.ProcessingJob;
import br.com.payroll.portability.portability.application.ProcessingJobRepository;
import br.com.payroll.portability.portability.domain.TechnicalJobStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcProcessingJobRepository implements ProcessingJobRepository {

  private static final RowMapper<ProcessingJob> MAPPER =
      (rs, rowNum) ->
          new ProcessingJob(
              UUID.fromString(rs.getString("id")),
              UUID.fromString(rs.getString("operation_id")),
              rs.getString("job_type"),
              TechnicalJobStatus.valueOf(rs.getString("technical_status")),
              rs.getInt("attempt_count"),
              rs.getString("lease_owner"),
              rs.getTimestamp("lease_until") == null
                  ? null
                  : rs.getTimestamp("lease_until").toInstant(),
              rs.getLong("fence_token"),
              rs.getTimestamp("next_run_at").toInstant(),
              rs.getString("last_error"));

  private final JdbcTemplate jdbc;

  public JdbcProcessingJobRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional
  public Optional<ProcessingJob> claimNext(String workerId, Instant now, Instant leaseUntil) {
    List<UUID> ids =
        jdbc.query(
            """
            SELECT id FROM processing_jobs
            WHERE technical_status IN ('PENDING', 'FAILED')
              AND next_run_at <= ?
              AND (lease_until IS NULL OR lease_until < ?)
            ORDER BY next_run_at
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """,
            (rs, rowNum) -> UUID.fromString(rs.getString("id")),
            Timestamp.from(now),
            Timestamp.from(now));
    if (ids.isEmpty()) {
      return Optional.empty();
    }
    UUID jobId = ids.getFirst();
    jdbc.update(
        """
        UPDATE processing_jobs
        SET technical_status = 'RUNNING',
            lease_owner = ?,
            lease_until = ?,
            fence_token = fence_token + 1,
            updated_at = ?
        WHERE id = ?
        """,
        workerId,
        Timestamp.from(leaseUntil),
        Timestamp.from(now),
        jobId);
    return jdbc
        .query("SELECT * FROM processing_jobs WHERE id = ?", MAPPER, jobId)
        .stream()
        .findFirst();
  }

  @Override
  public void markSucceeded(UUID jobId, long fenceToken, Instant now) {
    jdbc.update(
        """
        UPDATE processing_jobs
        SET technical_status = 'SUCCEEDED',
            lease_owner = NULL,
            lease_until = NULL,
            last_error = NULL,
            updated_at = ?
        WHERE id = ? AND fence_token = ?
        """,
        Timestamp.from(now),
        jobId,
        fenceToken);
  }

  @Override
  public void reschedule(
      UUID jobId,
      long fenceToken,
      String jobType,
      Instant nextRunAt,
      int attemptCount,
      String lastError,
      Instant now) {
    jdbc.update(
        """
        UPDATE processing_jobs
        SET technical_status = 'PENDING',
            job_type = ?,
            next_run_at = ?,
            attempt_count = ?,
            last_error = ?,
            lease_owner = NULL,
            lease_until = NULL,
            updated_at = ?
        WHERE id = ? AND fence_token = ?
        """,
        jobType,
        Timestamp.from(nextRunAt),
        attemptCount,
        lastError,
        Timestamp.from(now),
        jobId,
        fenceToken);
  }

  @Override
  public void markExhausted(UUID jobId, long fenceToken, String lastError, Instant now) {
    jdbc.update(
        """
        UPDATE processing_jobs
        SET technical_status = 'EXHAUSTED',
            last_error = ?,
            lease_owner = NULL,
            lease_until = NULL,
            updated_at = ?
        WHERE id = ? AND fence_token = ?
        """,
        lastError,
        Timestamp.from(now),
        jobId,
        fenceToken);
  }

  @Override
  public Optional<String> findIdempotencyKey(UUID operationId) {
    var list =
        jdbc.query(
            """
            SELECT idempotency_key FROM idempotency_keys
            WHERE operation_id = ? AND scope = 'portability-create'
            """,
            (rs, rowNum) -> rs.getString("idempotency_key"),
            operationId);
    return list.stream().findFirst();
  }
}
