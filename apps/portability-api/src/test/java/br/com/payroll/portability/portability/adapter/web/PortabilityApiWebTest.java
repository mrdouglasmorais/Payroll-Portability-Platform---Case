package br.com.payroll.portability.portability.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.payroll.portability.consent.application.ConsentRepository;
import br.com.payroll.portability.consent.domain.Consent;
import br.com.payroll.portability.integration.central.CentralAgendaItem;
import br.com.payroll.portability.integration.central.CentralGateway;
import br.com.payroll.portability.integration.central.CentralPortabilityRequest;
import br.com.payroll.portability.integration.central.CentralStatus;
import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import br.com.payroll.portability.schedule.domain.InstallmentStatus;
import br.com.payroll.portability.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PortabilityApiWebTest {

  @Container
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("portability")
          .withUsername("portability")
          .withPassword("portability");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("app.central.worker-enabled", () -> "false");
  }

  @TestConfiguration
  static class FakeCentralConfig {
    @Bean
    @Primary
    CentralGateway webFakeCentral() {
      return new CentralGateway() {
        @Override
        public String createAuthorization(
            String beneficiaryExternalRef, String agreementId, String correlationId) {
          return "auth-web";
        }

        @Override
        public List<CentralAgendaItem> fetchAgenda(
            String beneficiaryExternalRef, String correlationId) {
          return List.of(
              new CentralAgendaItem(
                  "parcela-web-1",
                  "conv-1",
                  new BigDecimal("10.00"),
                  LocalDate.of(2026, 12, 1),
                  "OPEN"));
        }

        @Override
        public String submitPortability(
            CentralPortabilityRequest request, String idempotencyKey, String correlationId) {
          return "prot-web";
        }

        @Override
        public CentralStatus pollStatus(String protocol, String correlationId) {
          return CentralStatus.pending();
        }
      };
    }
  }

  @Autowired WebApplicationContext context;
  @Autowired ConsentRepository consentRepository;
  @Autowired InstallmentRepository installmentRepository;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;

  private MockMvc mockMvc;
  private UUID beneficiaryId;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    jdbc.update("DELETE FROM idempotency_keys");
    jdbc.update("DELETE FROM processing_jobs");
    jdbc.update("DELETE FROM operation_events");
    jdbc.update("DELETE FROM installment_reservations");
    jdbc.update("DELETE FROM operation_installments");
    jdbc.update("DELETE FROM portability_operations");
    jdbc.update("DELETE FROM installment_snapshots");
    jdbc.update("DELETE FROM consents");
    jdbc.update("DELETE FROM beneficiaries");

    beneficiaryId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO beneficiaries (id, external_ref, display_name) VALUES (?, ?, ?)",
        beneficiaryId,
        "ben-web",
        "Web Beneficiary");

    Instant now = Instant.now(clock);
    Consent consent = Consent.request(beneficiaryId, "conv-1", now);
    consent.accept("auth-local", now);
    consentRepository.save(consent);

    installmentRepository.save(
        new InstallmentSnapshot(
            UUID.randomUUID(),
            "parcela-web-1",
            beneficiaryId,
            "conv-1",
            Money.of("10.00"),
            LocalDate.of(2026, 12, 1),
            InstallmentStatus.OPEN));
  }

  @Test
  void createReturns202AndGetStatus() throws Exception {
    String body =
        """
        {
          "beneficiarioId":"ben-web",
          "parcelas":["parcela-web-1"],
          "credorCessionario":"creditor-x",
          "contaRecebimento":{"banco":"001","agencia":"1234","conta":"999-0"}
        }
        """;

    String response =
        mockMvc
            .perform(
                post("/api/v1/operacoes-portabilidade")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", "web-idem-1")
                    .header("X-Correlation-Id", "corr-web")
                    .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.operationId").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertTrue(response.contains("operationId"));
    String operationId = response.replaceAll("(?s).*\"operationId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    mockMvc
        .perform(get("/api/v1/operacoes-portabilidade/" + operationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDENTE"));

    mockMvc
        .perform(get("/api/v1/operacoes-portabilidade/" + operationId + "/eventos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].eventType").value("OPERATION_CREATED"));
  }

  @Test
  void idempotencyMismatchReturns409() throws Exception {
    String body1 =
        """
        {
          "beneficiarioId":"ben-web",
          "parcelas":["parcela-web-1"],
          "credorCessionario":"creditor-x",
          "contaRecebimento":{"banco":"001","agencia":"1234","conta":"999-0"}
        }
        """;
    mockMvc
        .perform(
            post("/api/v1/operacoes-portabilidade")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "web-idem-conflict")
                .content(body1))
        .andExpect(status().isAccepted());

    String body2 =
        """
        {
          "beneficiarioId":"ben-web",
          "parcelas":["parcela-web-1"],
          "credorCessionario":"other-creditor",
          "contaRecebimento":{"banco":"001","agencia":"1234","conta":"999-0"}
        }
        """;
    mockMvc
        .perform(
            post("/api/v1/operacoes-portabilidade")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "web-idem-conflict")
                .content(body2))
        .andExpect(status().isConflict());
  }

  @Test
  void authorizationEndpointReturns201() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/autorizacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiarioId\":\"ben-new\",\"convenioId\":\"conv-9\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.autorizacaoId").value("auth-web"));

    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM beneficiaries WHERE external_ref = 'ben-new'", Integer.class);
    assertEquals(1, count);
  }
}
