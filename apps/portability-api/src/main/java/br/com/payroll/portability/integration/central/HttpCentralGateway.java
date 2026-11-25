package br.com.payroll.portability.integration.central;

import br.com.payroll.portability.integration.integrity.HmacCanonical;
import br.com.payroll.portability.integration.integrity.HmacSigner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpCentralGateway implements CentralGateway {

  private final RestClient restClient;
  private final HmacSigner signer;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public HttpCentralGateway(
      RestClient.Builder restClientBuilder,
      CentralProperties properties,
      HmacSigner signer,
      ObjectMapper objectMapper,
      Clock clock) {
    this.restClient =
        restClientBuilder.baseUrl(properties.getBaseUrl().replaceAll("/$", "")).build();
    this.signer = signer;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  public String createAuthorization(
      String beneficiaryExternalRef, String agreementId, String correlationId) {
    Map<String, String> body = new LinkedHashMap<>();
    body.put("beneficiarioId", beneficiaryExternalRef);
    body.put("convenioId", agreementId);
    byte[] raw = writeJson(body);
    ResponseEntity<byte[]> response =
        exchange("POST", "/autorizacoes", "", raw, null, correlationId);
    JsonNode json = readJson(response.getBody());
    String authId = text(json, "autorizacaoId");
    if (authId == null || authId.isBlank()) {
      throw new CentralException("Central authorization missing autorizacaoId", false, 502);
    }
    return authId;
  }

  @Override
  public List<CentralAgendaItem> fetchAgenda(String beneficiaryExternalRef, String correlationId) {
    String path = "/beneficiarios/" + beneficiaryExternalRef + "/agenda";
    ResponseEntity<byte[]> response = exchange("GET", path, "", new byte[0], null, correlationId);
    JsonNode root = readJson(response.getBody());
    if (!root.isArray()) {
      throw new CentralException("Central agenda response is not an array", false, 502);
    }
    List<CentralAgendaItem> items = new ArrayList<>();
    root.forEach(
        node ->
            items.add(
                new CentralAgendaItem(
                    text(node, "parcelaId"),
                    text(node, "convenioId"),
                    node.path("valor").decimalValue(),
                    LocalDate.parse(text(node, "dataDesconto")),
                    text(node, "status"))));
    return items;
  }

  @Override
  public String submitPortability(
      CentralPortabilityRequest request, String idempotencyKey, String correlationId) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("beneficiarioId", request.beneficiarioId());
    body.put("parcelas", request.parcelas());
    body.put("credorCessionario", request.credorCessionario());
    Map<String, String> account = new LinkedHashMap<>();
    account.put("banco", request.banco());
    account.put("agencia", request.agencia());
    account.put("conta", request.conta());
    body.put("contaRecebimento", account);
    byte[] raw = writeJson(body);
    ResponseEntity<byte[]> response =
        exchange("POST", "/operacoes-portabilidade", "", raw, idempotencyKey, correlationId);
    JsonNode json = readJson(response.getBody());
    String protocol = text(json, "protocolo");
    if (protocol == null || protocol.isBlank()) {
      throw new CentralException("Central portability missing protocolo", false, 502);
    }
    return protocol;
  }

  @Override
  public CentralStatus pollStatus(String protocol, String correlationId) {
    String path = "/operacoes-portabilidade/" + protocol;
    ResponseEntity<byte[]> response = exchange("GET", path, "", new byte[0], null, correlationId);
    if (response.getStatusCode().value() == 204) {
      return CentralStatus.pending();
    }
    JsonNode json = readJson(response.getBody());
    String status = text(json, "status");
    String reason = json.path("motivoRejeicao").isNull() ? null : text(json, "motivoRejeicao");
    if (status == null || status.isBlank()) {
      throw new CentralException("Central status missing status field", true, 502);
    }
    if ("PENDENTE".equals(status)) {
      return CentralStatus.pending();
    }
    return CentralStatus.terminal(status, reason);
  }

  private ResponseEntity<byte[]> exchange(
      String method,
      String path,
      String query,
      byte[] body,
      String idempotencyKey,
      String correlationId) {
    String timestamp = Long.toString(clock.instant().toEpochMilli());
    String nonce = UUID.randomUUID().toString();
    String keyId = signer.keyId();
    String corr = correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId;
    String canonical =
        HmacCanonical.requestString(
            method, path, query, body, timestamp, nonce, keyId, corr, idempotencyKey);
    String signature = signer.sign(canonical);

    try {
      var spec =
          restClient
              .method(HttpMethod.valueOf(method))
              .uri(path + (query == null || query.isBlank() ? "" : "?" + query))
              .headers(
                  headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("X-Signature", signature);
                    headers.set("X-Timestamp", timestamp);
                    headers.set("X-Nonce", nonce);
                    headers.set("X-Key-Id", keyId);
                    headers.set("X-Correlation-Id", corr);
                    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                      headers.set("Idempotency-Key", idempotencyKey);
                    }
                  });
      ResponseEntity<byte[]> response =
          (body == null || body.length == 0 ? spec.retrieve() : spec.body(body).retrieve())
              .toEntity(byte[].class);

      verifyResponse(method, path, query, response, nonce, corr);
      int code = response.getStatusCode().value();
      if (code >= 500) {
        throw new CentralException("Central unavailable: HTTP " + code, true, code);
      }
      if (code == 409) {
        throw new CentralException("Central conflict", false, 409);
      }
      if (code >= 400 && code != 204) {
        throw new CentralException("Central client error: HTTP " + code, false, code);
      }
      return response;
    } catch (CentralException ex) {
      throw ex;
    } catch (RestClientResponseException ex) {
      int code = ex.getStatusCode().value();
      boolean transientFailure = code == 429 || code >= 500;
      throw new CentralException(
          "Central HTTP error: " + code + " " + ex.getResponseBodyAsString(),
          transientFailure,
          code,
          ex);
    } catch (RestClientException ex) {
      throw new CentralException("Central transport failure", true, 0, ex);
    }
  }

  private void verifyResponse(
      String method,
      String path,
      String query,
      ResponseEntity<byte[]> response,
      String requestNonce,
      String correlationId) {
    String signature = response.getHeaders().getFirst("X-Signature");
    String timestamp = response.getHeaders().getFirst("X-Timestamp");
    String nonce = response.getHeaders().getFirst("X-Nonce");
    String keyId = response.getHeaders().getFirst("X-Key-Id");
    if (signature == null || timestamp == null || nonce == null || keyId == null) {
      throw new CentralException("Central response missing HMAC headers", true, 502);
    }
    byte[] body = response.getBody() == null ? new byte[0] : response.getBody();
    String canonical =
        HmacCanonical.responseString(
            method,
            path,
            query,
            body,
            timestamp,
            nonce,
            keyId,
            correlationId,
            requestNonce,
            response.getStatusCode().value());
    if (!signer.verify(canonical, signature)) {
      throw new CentralException("Central response signature invalid", true, 502);
    }
  }

  private byte[] writeJson(Object value) {
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (Exception ex) {
      throw new IllegalStateException("JSON serialization failed", ex);
    }
  }

  private JsonNode readJson(byte[] body) {
    try {
      return objectMapper.readTree(body == null ? new byte[0] : body);
    } catch (Exception ex) {
      throw new CentralException("Malformed Central JSON", true, 502, ex);
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    return value.asString();
  }
}
