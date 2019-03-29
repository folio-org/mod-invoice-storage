package org.folio.rest.impl;

import org.junit.Test;

import java.net.MalformedURLException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class InvoiceNumberTest extends TestBase {

  private static final String SEQUENCE_NUMBER = "sequenceNumber";
  private static final String INVOICE_NUMBER_ENDPOINT = "/invoice-storage/invoice-number";

  @Test
  public void testGetInvoiceNumber() throws MalformedURLException {
    long[] invoiceNumbers = new long[]{getNumberAsLong(), getNumberAsLong(), getNumberAsLong()};
    assertThat(invoiceNumbers[1] - invoiceNumbers[0], equalTo(1L));
    assertThat(invoiceNumbers[2] - invoiceNumbers[0], equalTo(2L));
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
