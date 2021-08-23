package org.folio.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PostgresClient;

public class MigrationService {

  private static final Logger log = LogManager.getLogger(MigrationService.class);
  private final OrdersStorageService ordersStorageService;

  public MigrationService(OrdersStorageService ordersStorageService) {
    this.ordersStorageService = ordersStorageService;
  }

  //  Migration method(s):
  public Future<Void> addOrderPoNumberToInvoicePoNumber (DBClient client) {
    Promise<Void> promise = Promise.promise();
    String schemaName = PostgresClient.convertToPsqlStandard(client.getTenantId());


    String sql = "DO\n" + "$$\n" + "begin\n" + " PERFORM %s.update_invoices_with_po_number('%s');\n" + "end;\n"
      + "$$ LANGUAGE plpgsql;";

    return promise.future();
  }

  //  Utility method(s):
  public String replaceSingleQuote(String inputString) {
    return inputString != null ? inputString.replace("'", "''") : null;
  }
}
