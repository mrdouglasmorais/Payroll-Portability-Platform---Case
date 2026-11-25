package br.com.payroll.central.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.payroll.central.integrity.HmacCanonical;
import br.com.payroll.central.integrity.HmacService;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CentralHmacIntegrationTest {

  @Container
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("central")
          .withUsername("central")
          .withPassword("central");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("app.hmac-secret", () -> "local-test-hmac-secret");
    registry.add("app.sim.confirm-delay", () -> "PT0S");
  }

  @Autowired WebApplicationContext context;
  @Autowired HmacService hmacService;
  @Autowired Clock clock;
  @Autowired HmacFilter hmacFilter;

  private MockMvc mockMvc() {
    return MockMvcBuilders.webAppContextSetup(context).addFilters(hmacFilter).build();
  }

  @Test
  void rejectsTamperedBody() throws Exception {
    String body = "{\"beneficiarioId\":\"ben-001\",\"convenioId\":\"conv-1\"}";
    var req =
        signedPost("/autorizacoes", body, null, "")
            .content("{\"beneficiarioId\":\"ben-999\",\"convenioId\":\"conv-1\"}");
    mockMvc().perform(req).andExpect(status().isForbidden());
  }

  @Test
  void rejectsReplayNonce() throws Exception {
    String body = "{\"beneficiarioId\":\"ben-001\",\"convenioId\":\"conv-1\"}";
    String nonce = "fixed-nonce-" + UUID.randomUUID();
    mockMvc()
        .perform(signedPost("/autorizacoes", body, nonce, ""))
        .andExpect(status().isCreated());
    mockMvc()
        .perform(signedPost("/autorizacoes", body, nonce, ""))
        .andExpect(status().isForbidden());
  }

  @Test
  void portabilityIdempotencyAndConfirmation() throws Exception {
    authorize("ben-001", "conv-1");
    String body =
        compact(
            """
            {"beneficiarioId":"ben-001","parcelas":["parcela-seed-1"],"credorCessionario":"bank-x",
            "contaRecebimento":{"banco":"001","agencia":"1234","conta":"99999"}}
            """);
    String idem = "idem-" + UUID.randomUUID();
    MvcResult first =
        mockMvc()
            .perform(signedPost("/operacoes-portabilidade", body, null, idem))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.protocolo").exists())
            .andReturn();
    String protocol =
        tools.jackson.databind.json.JsonMapper.shared()
            .readTree(first.getResponse().getContentAsString())
            .path("protocolo")
            .asString();

    mockMvc()
        .perform(signedPost("/operacoes-portabilidade", body, null, idem))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.protocolo").value(protocol));

    mockMvc()
        .perform(signedGet("/operacoes-portabilidade/" + protocol))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMADA"));
  }

  @Test
  void simulatedRejectionFault() throws Exception {
    authorize("ben-001", "conv-1");
    String body =
        compact(
            """
            {"beneficiarioId":"ben-001","parcelas":["parcela-seed-2"],"credorCessionario":"bank-x",
            "contaRecebimento":{"banco":"001","agencia":"1234","conta":"99999"}}
            """);
    String idem = "idem-rej-" + UUID.randomUUID();
    MvcResult created =
        mockMvc()
            .perform(
                signedPost("/operacoes-portabilidade", body, null, idem)
                    .header("X-Sim-Fault", "REJECT"))
            .andExpect(status().isAccepted())
            .andReturn();
    String protocol =
        tools.jackson.databind.json.JsonMapper.shared()
            .readTree(created.getResponse().getContentAsString())
            .path("protocolo")
            .asString();

    MvcResult statusResult =
        mockMvc()
            .perform(signedGet("/operacoes-portabilidade/" + protocol))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJEITADA"))
            .andReturn();
    assertTrue(statusResult.getResponse().getContentAsString().contains("Simulated"));
  }

  private void authorize(String beneficiaryId, String agreementId) throws Exception {
    String body =
        "{\"beneficiarioId\":\"" + beneficiaryId + "\",\"convenioId\":\"" + agreementId + "\"}";
    mockMvc().perform(signedPost("/autorizacoes", body, null, "")).andExpect(status().isCreated());
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder signedPost(
      String path, String body, String nonce, String idempotencyKey) {
    String effectiveNonce = nonce == null ? UUID.randomUUID().toString() : nonce;
    String timestamp = Long.toString(clock.instant().toEpochMilli());
    String correlation = "corr-" + UUID.randomUUID();
    String canonical =
        HmacCanonical.requestString(
            "POST",
            path,
            "",
            body.getBytes(),
            timestamp,
            effectiveNonce,
            hmacService.keyId(),
            correlation,
            idempotencyKey == null ? "" : idempotencyKey);
    var builder =
        post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header("X-Signature", hmacService.sign(canonical))
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", effectiveNonce)
            .header("X-Key-Id", hmacService.keyId())
            .header("X-Correlation-Id", correlation);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      builder.header("Idempotency-Key", idempotencyKey);
    }
    return builder;
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder signedGet(
      String path) {
    String nonce = UUID.randomUUID().toString();
    String timestamp = Long.toString(clock.instant().toEpochMilli());
    String correlation = "corr-" + UUID.randomUUID();
    String canonical =
        HmacCanonical.requestString(
            "GET",
            path,
            "",
            new byte[0],
            timestamp,
            nonce,
            hmacService.keyId(),
            correlation,
            "");
    return get(path)
        .header("X-Signature", hmacService.sign(canonical))
        .header("X-Timestamp", timestamp)
        .header("X-Nonce", nonce)
        .header("X-Key-Id", hmacService.keyId())
        .header("X-Correlation-Id", correlation);
  }

  private static String compact(String json) {
    return json.replaceAll("\\s+", "");
  }
}
