package br.com.payroll.portability.shared.observability;

/** Structured log field names (MDC / key-value). Keep cardinality low for metric tags. */
public final class LogFields {

  public static final String EVENT = "event";
  public static final String OPERATION_ID = "operationId";
  public static final String JOB_ID = "jobId";
  public static final String CORRELATION_ID = "correlationId";
  public static final String ATTEMPT = "attempt";
  public static final String FENCE_TOKEN = "fenceToken";
  public static final String JOB_TYPE = "jobType";
  public static final String PROTOCOL = "protocol";
  public static final String ERROR_CODE = "errorCode";
  public static final String OUTCOME = "outcome";
  public static final String PARCELA_ID = "parcelaId";
  public static final String RECEIVING_BANK = "receivingBank";
  public static final String RECEIVING_AGENCY_MASKED = "receivingAgencyMasked";
  public static final String RECEIVING_ACCOUNT_MASKED = "receivingAccountMasked";
  public static final String REJECTION_REASON = "rejectionReason";

  private LogFields() {}
}
