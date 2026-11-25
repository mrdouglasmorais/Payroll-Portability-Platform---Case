package br.com.payroll.central.adapter.web;

import br.com.payroll.central.integrity.HmacCanonical;
import br.com.payroll.central.integrity.HmacProperties;
import br.com.payroll.central.integrity.HmacService;
import br.com.payroll.central.integrity.NonceReplayGuard;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class HmacFilter extends OncePerRequestFilter {

  private final HmacService hmacService;
  private final HmacProperties properties;
  private final NonceReplayGuard replayGuard;
  private final Clock clock;

  public HmacFilter(
      HmacService hmacService,
      HmacProperties properties,
      NonceReplayGuard replayGuard,
      Clock clock) {
    this.hmacService = hmacService;
    this.properties = properties;
    this.replayGuard = replayGuard;
    this.clock = clock;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator") || "/status".equals(path);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    byte[] body = request.getInputStream().readAllBytes();
    CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, body);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    String signature = header(wrappedRequest, "X-Signature");
    String timestamp = header(wrappedRequest, "X-Timestamp");
    String nonce = header(wrappedRequest, "X-Nonce");
    String keyId = header(wrappedRequest, "X-Key-Id");
    String correlationId = header(wrappedRequest, "X-Correlation-Id");
    String idempotencyKey = header(wrappedRequest, "Idempotency-Key");

    if (isBlank(signature)
        || isBlank(timestamp)
        || isBlank(nonce)
        || isBlank(keyId)
        || isBlank(correlationId)) {
      reject(wrappedRequest, wrappedResponse, "missing HMAC headers", correlationId, nonce);
      return;
    }

    if (!hmacService.keyId().equals(keyId)) {
      reject(wrappedRequest, wrappedResponse, "unknown key id", correlationId, nonce);
      return;
    }

    if (!timestampFresh(timestamp)) {
      reject(wrappedRequest, wrappedResponse, "expired timestamp", correlationId, nonce);
      return;
    }

    if (!replayGuard.registerIfNew(nonce)) {
      reject(wrappedRequest, wrappedResponse, "replayed nonce", correlationId, nonce);
      return;
    }

    String canonical =
        HmacCanonical.requestString(
            wrappedRequest.getMethod(),
            wrappedRequest.getRequestURI(),
            nullToEmpty(wrappedRequest.getQueryString()),
            body,
            timestamp,
            nonce,
            keyId,
            correlationId,
            nullToEmpty(idempotencyKey));

    if (!hmacService.verify(canonical, signature)) {
      reject(wrappedRequest, wrappedResponse, "invalid signature", correlationId, nonce);
      return;
    }

    wrappedRequest.setAttribute("hmac.requestNonce", nonce);
    wrappedRequest.setAttribute("hmac.correlationId", correlationId);

    filterChain.doFilter(wrappedRequest, wrappedResponse);
    signAndCopy(wrappedRequest, wrappedResponse, nonce, correlationId);
  }

  private void reject(
      HttpServletRequest request,
      ContentCachingResponseWrapper response,
      String message,
      String correlationId,
      String requestNonce)
      throws IOException {
    response.resetBuffer();
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getOutputStream().write(("{\"error\":\"" + message + "\"}").getBytes());
    signAndCopy(request, response, nullToEmpty(requestNonce), nullToEmpty(correlationId));
  }

  private void signAndCopy(
      HttpServletRequest request,
      ContentCachingResponseWrapper response,
      String requestNonce,
      String correlationId)
      throws IOException {
    byte[] responseBody = response.getContentAsByteArray();
    String responseNonce = UUID.randomUUID().toString();
    String responseTimestamp = Long.toString(clock.instant().toEpochMilli());
    String canonical =
        HmacCanonical.responseString(
            request.getMethod(),
            request.getRequestURI(),
            nullToEmpty(request.getQueryString()),
            responseBody,
            responseTimestamp,
            responseNonce,
            hmacService.keyId(),
            nullToEmpty(correlationId),
            nullToEmpty(requestNonce),
            response.getStatus());
    response.setHeader("X-Signature", hmacService.sign(canonical));
    response.setHeader("X-Timestamp", responseTimestamp);
    response.setHeader("X-Nonce", responseNonce);
    response.setHeader("X-Key-Id", hmacService.keyId());
    response.setHeader("X-Correlation-Id", nullToEmpty(correlationId));
    response.copyBodyToResponse();
  }

  private boolean timestampFresh(String timestamp) {
    try {
      Instant ts = Instant.ofEpochMilli(Long.parseLong(timestamp));
      return Duration.between(ts, clock.instant()).abs().compareTo(properties.getHmacMaxSkew()) <= 0;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private static String header(HttpServletRequest request, String name) {
    return request.getHeader(name);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
