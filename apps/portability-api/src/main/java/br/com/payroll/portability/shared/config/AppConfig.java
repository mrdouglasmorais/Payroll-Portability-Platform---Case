package br.com.payroll.portability.shared.config;

import br.com.payroll.portability.integration.central.CentralProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(CentralProperties.class)
public class AppConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

  @Bean
  TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }
}
