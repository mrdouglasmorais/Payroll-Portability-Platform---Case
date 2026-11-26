package br.com.payroll.portability.shared.web;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebHardeningConfig {

  @Bean
  CorsFilter corsFilter(
      @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
          List<String> allowedOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(false);
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    config.setAllowedHeaders(
        List.of("Content-Type", "Idempotency-Key", "X-Correlation-Id", "Accept"));
    config.setExposedHeaders(List.of("X-Correlation-Id"));
    config.setMaxAge(600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return new CorsFilter(source);
  }
}
