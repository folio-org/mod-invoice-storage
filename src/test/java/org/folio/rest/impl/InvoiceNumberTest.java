package org.folio.rest.impl;

import io.vertx.ext.unit.junit.Repeat;
import io.vertx.ext.unit.junit.RepeatRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class InvoiceNumberTest extends TestBase {

  @Rule
  public RepeatRule rule = new RepeatRule();
  private static List<Long> invoiceNumberList;

  private static final String SEQUENCE_NUMBER = "sequenceNumber";
  private static final String INVOICE_NUMBER_ENDPOINT = "/invoice-storage/invoice-number";

  @BeforeClass
  public static void setUp() {
    invoiceNumberList  = new ArrayList<>();
  }

  @Test
  @Repeat(3)
  public void testGetInvoiceNumber() throws MalformedURLException {
    invoiceNumberList.add(getNumberAsLong());
  }

  @AfterClass
  public static void tearDown() {
    assertThat(invoiceNumberList.get(1) - invoiceNumberList.get(0), equalTo(1L));
    assertThat(invoiceNumberList.get(2) - invoiceNumberList.get(0), equalTo(2L));
  }

  private long getNumberAsLong() throws MalformedURLException {
    return new Long(getData(INVOICE_NUMBER_ENDPOINT)
      .then()
        .statusCode(200)
        .extract()
          .response()
            .path(SEQUENCE_NUMBER));
  }
}
