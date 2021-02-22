package org.folio.rest.persist;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import one.util.streamex.StreamEx;

public class HelperUtils {
  private static final String IL_NUMBER_PREFIX = "ilNumber_";
  private static final String QUOTES_SYMBOL = "\"";
  private static final Pattern CQL_SORT_BY_PATTERN = Pattern.compile("(.*)(\\ssortBy\\s.*)", Pattern.CASE_INSENSITIVE);

  private HelperUtils() {
    throw new UnsupportedOperationException("Cannot instantiate utility class.");
  }

  public enum SequenceQuery {

    CREATE_SEQUENCE {
      @Override
      public String getQuery(String invoiceId) {
        return "CREATE SEQUENCE IF NOT EXISTS " + constructSequenceName(invoiceId) + " MINVALUE 1 MAXVALUE 999";
      }
    },
    GET_IL_NUMBER_FROM_SEQUENCE {
      @Override
      public String getQuery(String invoiceId) {
        return "SELECT * FROM NEXTVAL('" + constructSequenceName(invoiceId) + "')";
      }
    },
    DROP_SEQUENCE {
      @Override
      public String getQuery(String invoiceId) {
        return "DROP SEQUENCE IF EXISTS " + constructSequenceName(invoiceId);
      }
    };

    private static String constructSequenceName(String invoiceId) {
      return QUOTES_SYMBOL + IL_NUMBER_PREFIX + invoiceId + QUOTES_SYMBOL;
    }

    public abstract String getQuery(String invoiceId);
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
}
