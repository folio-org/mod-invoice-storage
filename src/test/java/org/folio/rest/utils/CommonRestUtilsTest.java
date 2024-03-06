package org.folio.rest.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;

class CommonRestUtilsTest {

  @Test
  public void test_cql_query_with_equal_operator() {
    Collection<String> ids = Arrays.asList("1", "2", "3");
    String idField = "id";
    String expectedQuery = "id==(1 or 2 or 3)";

    String actualQuery = CommonRestUtils.convertIdsToCqlQuery(ids, idField);

    assertEquals(expectedQuery, actualQuery);
  }

  @Test
  public void test_cql_query_with_or_operation() {
    Collection<String> values = Arrays.asList("value1", "value2", "value3");
    String fieldName = "field";
    boolean strictMatch = false;

    String expectedQuery = "field=(value1 or value2 or value3)";
    String actualQuery = CommonRestUtils.convertFieldListToCqlQuery(values, fieldName, strictMatch);

    assertEquals(expectedQuery, actualQuery);
  }

  @Test
  void test_returnListWhenAllFuturesCompleteSuccessfully() {
    List<Future<String>> futures = new ArrayList<>();
    Future<String> future1 = Future.succeededFuture("Result 1");
    Future<String> future2 = Future.succeededFuture("Result 2");
    futures.add(future1);
    futures.add(future2);

    Future<List<String>> result = CommonRestUtils.collectResultsOnSuccess(futures);

    assertTrue(result.succeeded());
    assertEquals(2, result.result().size());
    assertEquals("Result 1", result.result().get(0));
    assertEquals("Result 2", result.result().get(1));
  }
}
