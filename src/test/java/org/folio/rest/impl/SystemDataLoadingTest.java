package org.folio.rest.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

public class SystemDataLoadingTest extends TestBase {

  private final Logger logger = LoggerFactory.getLogger(SystemDataLoadingTest.class);

  @Test
  public void systemDataWasLoaded() throws MalformedURLException {
    for(TestEntities entity: TestEntities.getCollectableEntities()) {
      logger.info(String.format("--- mod-invoice-storage %s test: Verifying system data was loaded ... ", entity.name()));
      verifyCollectionQuantity(entity.getEndpoint(), entity.getEstimatedSystemDataRecordsQuantity());
    }
  }
}
