package org.folio.config;

import org.folio.dao.invoice.InvoiceDAO;
import org.folio.dao.invoice.InvoicePostgresDAO;
import org.folio.dao.lines.InvoiceLinesDAO;
import org.folio.dao.lines.InvoiceLinesPostgresDAO;
import org.folio.rest.core.RestClient;
import org.folio.service.InvoiceLineNumberService;
import org.folio.service.InvoiceStorageService;
import org.folio.service.order.OrderStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

  @Bean
  RestClient restClient() {
    return new RestClient();
  }

  @Bean OrderStorageService ordersStorageService(RestClient restClient) {
    return new OrderStorageService(restClient);
  }

  @Bean
  public InvoiceDAO invoiceDAO() {
    return new InvoicePostgresDAO();
  }

  @Bean
  public InvoiceLinesDAO invoiceLinesDAO() {
    return new InvoiceLinesPostgresDAO();
  }

  @Bean
  public InvoiceStorageService invoiceStorageService(InvoiceDAO invoiceDAO) {
    return new InvoiceStorageService(invoiceDAO);
  }

  @Bean
  public InvoiceLineNumberService invoiceLineNumberService(InvoiceDAO invoiceDAO, InvoiceLinesDAO invoiceLinesDAO) {
    return new InvoiceLineNumberService(invoiceDAO, invoiceLinesDAO);
  }
}
