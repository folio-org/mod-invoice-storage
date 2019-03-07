package org.folio.rest.impl;

import io.restassured.http.Header;
import io.vertx.core.Vertx;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

/**
 * When not run from StorageTestSuite then this class invokes StorageTestSuite.before() and
 * StorageTestSuite.after() to allow to run a single test class, for example from within an
 * IDE during development.
 */
public abstract class TestBase {

  private static boolean invokeStorageTestSuiteAfter = false;

  static final String NON_EXISTED_ID = "bad500aa-aaaa-500a-aaaa-aaaaaaaaaaaa";
  static final Header TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "diku");

  @BeforeClass
  public static void testBaseBeforeClass() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    Vertx vertx = StorageTestSuite.getVertx();
    if (vertx == null) {
      invokeStorageTestSuiteAfter = true;
      StorageTestSuite.before();
    }

  }

  @AfterClass
  public static void testBaseAfterClass()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    if (invokeStorageTestSuiteAfter) {
      StorageTestSuite.after();
    }
  }

  String getFile(String filename) {
    String value = "";
    try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(filename)) {
      if (inputStream != null) {
        value = IOUtils.toString(inputStream, "UTF-8");
      }
    } catch (Exception e) {
      value = "";
    }
    return value;
  }

}
