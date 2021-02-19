package org.folio.rest.impl;

import java.net.MalformedURLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.Test;

public class SystemDataLoadingTest extends TestBase {

  private static final Logger LOGGER = LogManager.getLogger(SystemDataLoadingTest.class);

  @Test
  public void systemDataWasLoaded() throws MalformedURLException {
    for(TestEntities entity: TestEntities.getCollectableEntities()) {
      LOGGER.info(String.format("--- mod-invoice-storage %s test: Verifying system data was loaded ... ", entity.name()));
      verifyCollectionQuantity(entity.getEndpoint(), entity.getEstimatedSystemDataRecordsQuantity());
    }
  }
}
