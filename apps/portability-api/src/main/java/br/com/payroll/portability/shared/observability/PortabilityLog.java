package br.com.payroll.portability.shared.observability;

import org.slf4j.Logger;

/** Thin helper for SLF4J fluent structured events. */
public final class PortabilityLog {

  private PortabilityLog() {}

  public static void info(Logger log, String event, String message) {
    log.atInfo().addKeyValue(LogFields.EVENT, event).log(message);
  }

  public static void warn(Logger log, String event, String message) {
    log.atWarn().addKeyValue(LogFields.EVENT, event).log(message);
  }

  public static void error(Logger log, String event, String message) {
    log.atError().addKeyValue(LogFields.EVENT, event).log(message);
  }
}
