package org.folio.service.migration;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.MigrationService;
import org.folio.service.OrdersStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

public class MigrationServiceTest {

  public MigrationService migrationService;

  @Mock
  public OrdersStorageService ordersStorageService;

  @Mock
  public DBClient dbClient;

  @Mock
  private PostgresClient postgresClient;

  private Map<String, String> okapiHeaders = new HashMap<>();

  @Test
  void testShouldCompleteIfAllExecuted(VertxTestContext testContext) {}
  Vertx vertx = Vertx.vertx();

}
