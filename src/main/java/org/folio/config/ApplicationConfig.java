package org.folio.config;

import org.folio.dao.audit.AuditOutboxEventLogDAO;
import org.folio.dao.audit.AuditOutboxEventLogPostgresDAO;
import org.folio.dao.invoice.InvoiceDAO;
import org.folio.dao.invoice.InvoicePostgresDAO;
import org.folio.dao.lines.InvoiceLinesDAO;
import org.folio.dao.lines.InvoiceLinesPostgresDAO;
import org.folio.kafka.KafkaConfig;
import org.folio.rest.core.RestClient;
import org.folio.service.InvoiceLineNumberService;
import org.folio.service.InvoiceLineStorageService;
import org.folio.service.InvoiceStorageService;
import org.folio.service.adjustment.AdjustmentPresetsService;
import org.folio.service.audit.AuditEventProducer;
import org.folio.service.audit.AuditOutboxService;
import org.folio.service.order.OrderStorageService;
import org.folio.service.setting.SettingsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(KafkaConfiguration.class)
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
  public AuditOutboxEventLogDAO auditOutboxEventLogDAO() {
    return new AuditOutboxEventLogPostgresDAO();
  }

  @Bean
  public InvoiceStorageService invoiceStorageService(InvoiceDAO invoiceDAO, AuditOutboxService auditOutboxService) {
    return new InvoiceStorageService(invoiceDAO, auditOutboxService);
  }

  @Bean
  public InvoiceLineStorageService invoiceLineStorageService(InvoiceLinesDAO invoiceLinesDAO, AuditOutboxService auditOutboxService) {
    return new InvoiceLineStorageService(invoiceLinesDAO, auditOutboxService);
  }

  @Bean
  public InvoiceLineNumberService invoiceLineNumberService(InvoiceDAO invoiceDAO, InvoiceLinesDAO invoiceLinesDAO) {
    return new InvoiceLineNumberService(invoiceDAO, invoiceLinesDAO);
  }

  @Bean
  public AuditEventProducer auditEventProducer(KafkaConfig kafkaConfig) {
    return new AuditEventProducer(kafkaConfig);
  }

  @Bean
  public AuditOutboxService auditOutboxService(AuditOutboxEventLogDAO auditOutboxEventLogDAO, AuditEventProducer producer) {
    return new AuditOutboxService(auditOutboxEventLogDAO, producer);
  }

  @Bean
  public AdjustmentPresetsService adjustmentPresetsService() {
    return new AdjustmentPresetsService();
  }

  @Bean
  public SettingsService settingsService() {
    return new SettingsService();
  }
}
