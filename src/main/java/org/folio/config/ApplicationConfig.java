package org.folio.config;

import org.folio.rest.core.RestClient;
import org.folio.service.OrdersStorageService;
import org.folio.service.MigrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DAOConfiguration.class, ServicesConfiguration.class})
public class ApplicationConfig {

  @Bean
  RestClient restClient() {
    return new RestClient();
  }

  @Bean
  OrdersStorageService ordersStorageService(RestClient restClient) {
    return new OrdersStorageService(restClient);
  }

  @Bean
  MigrationService migrationService(OrdersStorageService ordersStorageService) {
    return new MigrationService(ordersStorageService);
  }

}
