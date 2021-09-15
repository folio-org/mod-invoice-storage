package org.folio.service.migration.models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "invoiceId",
  "poNumbers"
})

public class InvoiceUpdateDto {

  @JsonProperty("invoiceId")
  @JsonPropertyDescription("Invoice ID")
  @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
  private String invoiceId;

  @JsonProperty("poNumbers")
  @JsonPropertyDescription("List of PO numbers")
  @Valid
  private List<String> poNumbers = new ArrayList<>();

  @JsonProperty("invoiceId")
  public String getInvoiceId() {
    return invoiceId;
  }

  @JsonProperty("poNumbers")
  public void setInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
  }

  public InvoiceUpdateDto withInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
    return this;
  }

  @JsonProperty("poNumbers")
  public List<String> getPoNumbers() {
    return poNumbers;
  }

  @JsonProperty("poNumbers")
  public void setPoNumbers(List<String> poNumbers) {
    this.poNumbers = poNumbers;
  }

  public InvoiceUpdateDto withPoNumbers(List<String> poNumbers) {
    this.poNumbers = poNumbers;
    return this;
  }

}
