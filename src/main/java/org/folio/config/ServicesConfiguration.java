package org.folio.config;

import org.folio.dao.invoice.InvoiceDAO;
import org.folio.service.InvoiceStorageService;
import org.springframework.context.annotation.Bean;

public class ServicesConfiguration {

  @Bean
  public InvoiceStorageService invoiceStorageService(InvoiceDAO invoiceDAO) {
    return new InvoiceStorageService(invoiceDAO);
  }

}
