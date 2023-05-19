package org.folio.rest.utils;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.HttpException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import one.util.streamex.StreamEx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HelperUtils {
  private static final Logger log = LogManager.getLogger(HelperUtils.class);

  private static final Pattern CQL_SORT_BY_PATTERN = Pattern.compile("(.*)(\\ssortBy\\s.*)", Pattern.CASE_INSENSITIVE);

  private HelperUtils() {
    throw new UnsupportedOperationException("Cannot instantiate utility class.");
  }

  public static String combineCqlExpressions(String term, String... expressions) {
    if (ArrayUtils.isEmpty(expressions)) {
      return EMPTY;
    }

    String sorting = EMPTY;

    // Check whether last expression contains sorting query. If it does, extract it to be added in the end of the resulting query
    Matcher matcher = CQL_SORT_BY_PATTERN.matcher(expressions[expressions.length - 1]);
    if (matcher.find()) {
      expressions[expressions.length - 1] = matcher.group(1);
      sorting = matcher.group(2);
    }

    return StreamEx.of(expressions)
      .filter(StringUtils::isNotBlank)
      .joining(") " + term + " (", "(", ")") + sorting;
  }

  public static JsonObject verifyAndExtractBody(org.folio.rest.tools.client.Response response) {
    verifyResponse(response);
    return response.getBody();
  }

  public static void verifyResponse(org.folio.rest.tools.client.Response response) {
    if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
      throw new CompletionException(
        new HttpException(response.getCode(), response.getError().getString("errorMessage")));
    }
  }

  public static String encodeQuery(String query) {
    log.debug("encodeQuery: query={}", query);
    try {
      return URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      log.error("Error happened while attempting to encode '{}'", query, e);
      throw new CompletionException(e);
    }
  }
}
