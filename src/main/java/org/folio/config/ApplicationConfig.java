package org.folio.config;

import org.folio.dao.invoice.InvoiceDAO;
import org.folio.dao.invoice.InvoicePostgresDAO;
import org.folio.rest.core.RestClient;
import org.folio.service.InvoiceStorageService;
import org.folio.service.migration.MigrationService;
import org.folio.service.order.OrdersStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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

  @Bean
  public InvoiceDAO invoiceDAO() {
    return new InvoicePostgresDAO();
  }

  @Bean
  public InvoiceStorageService invoiceStorageService(InvoiceDAO invoiceDAO) {
    return new InvoiceStorageService(invoiceDAO);
  }
}
