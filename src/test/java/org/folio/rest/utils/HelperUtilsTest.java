package org.folio.rest.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletionException;

import org.folio.rest.tools.client.Response;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class HelperUtilsTest {

  @Test
  void testShouldEncodeQuery() {
    assertThat(HelperUtils.encodeQuery("?limit=123&offset=0"), is("%3Flimit%3D123%26offset%3D0"));
  }

  @Test
  void testShouldThrowCompletionExceptionForSpecificCodeRange() {
    Response response = new Response();
    response.setCode(100);
    JsonObject error = new JsonObject();
    response.setError(error);
    assertThrows(CompletionException.class, () -> {
      HelperUtils.verifyResponse(response);
    });
  }
}
