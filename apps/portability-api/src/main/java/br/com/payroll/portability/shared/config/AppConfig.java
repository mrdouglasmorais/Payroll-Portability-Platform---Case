package br.com.payroll.portability.shared.config;

import br.com.payroll.portability.integration.central.CentralProperties;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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
  RestClient.Builder restClientBuilder(CentralProperties properties) {
    Duration connect =
        properties.getConnectTimeout() == null
            ? Duration.ofSeconds(2)
            : properties.getConnectTimeout();
    Duration read =
        properties.getReadTimeout() == null
            ? Duration.ofSeconds(5)
            : properties.getReadTimeout();
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connect).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(read);
    return RestClient.builder().requestFactory(factory);
  }

  @Bean
  TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }
}
