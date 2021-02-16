package org.folio.config;

import org.folio.dao.invoice.InvoiceDAO;
import org.folio.dao.invoice.InvoicePostgresDAO;
import org.springframework.context.annotation.Bean;

public class DAOConfiguration {

  @Bean
  public InvoiceDAO invoiceDAO() {
    return new InvoicePostgresDAO();
  }

}
