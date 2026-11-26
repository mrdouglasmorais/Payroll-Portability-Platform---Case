package br.com.payroll.portability.shared.observability;

/** Stable operational log event names (not audit ledger). */
public final class LogEvents {

  public static final String PORTABILITY_CREATED = "portability.created";
  public static final String CENTRAL_SUBMIT_ACCEPTED = "central.submit.accepted";
  public static final String CENTRAL_POLL_PENDING = "central.poll.pending";
  public static final String PORTABILITY_CONFIRMED = "portability.confirmed";
  public static final String PORTABILITY_REJECTED = "portability.rejected";
  public static final String CENTRAL_CALL_FAILED = "central.call.failed";
  /** Central refused agenda (typically missing ACCEPTED authorization). */
  public static final String CENTRAL_AGENDA_FORBIDDEN = "central.agenda.forbidden";
  public static final String JOB_EXHAUSTED = "job.exhausted";
  public static final String JOB_FENCE_STALE = "job.fence.stale";
  public static final String IDEMPOTENCY_REPLAY = "idempotency.replay";
  public static final String IDEMPOTENCY_CONFLICT = "idempotency.conflict";
  public static final String RESERVATION_CONFLICT = "reservation.conflict";

  private LogEvents() {}
}
