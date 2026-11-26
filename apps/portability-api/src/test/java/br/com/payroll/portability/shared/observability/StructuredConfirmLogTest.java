package br.com.payroll.portability.shared.observability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class StructuredConfirmLogTest {

  @Test
  void confirmedEventWithoutFullAccount(CapturedOutput output) {
    String fullAccount = "99999-0";
    var log = LoggerFactory.getLogger("portability.confirm.probe");
    log.atInfo()
        .addKeyValue(LogFields.EVENT, LogEvents.PORTABILITY_CONFIRMED)
        .addKeyValue(LogFields.RECEIVING_BANK, "001")
        .addKeyValue(LogFields.RECEIVING_AGENCY_MASKED, SensitiveData.maskAgency("1234"))
        .addKeyValue(LogFields.RECEIVING_ACCOUNT_MASKED, SensitiveData.maskAccount(fullAccount))
        .log("Portability confirmed");

    String text = output.getOut() + output.getErr();
    assertTrue(text.contains(LogEvents.PORTABILITY_CONFIRMED) || text.contains("Portability confirmed"));
    assertTrue(text.contains(SensitiveData.maskAccount(fullAccount)));
    assertFalse(text.contains(fullAccount));
  }
}
