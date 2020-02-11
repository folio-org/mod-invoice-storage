package org.folio.rest.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.MalformedURLException;

public class SystemDataLoadingTest extends TestBase {

  private final Logger logger = LoggerFactory.getLogger(SystemDataLoadingTest.class);

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void systemDataWasLoaded(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Verifying system data was loaded ... ", testEntity.name()));
    verifyCollectionQuantity(testEntity.getEndpoint(), testEntity.getSystemDataQuantity());
  }
}
