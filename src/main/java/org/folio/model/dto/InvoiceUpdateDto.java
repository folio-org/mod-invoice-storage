package org.folio.model.dto;

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
  "purchaseOrderId",
  "poNumber",
  "invoiceIds"

})

public class InvoiceUpdateDto {

  @JsonProperty("purchaseOrderId")
  @JsonPropertyDescription("Purchase order ID")
  @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
  private String purchaseOrderId;

  @JsonProperty("poNumber")
  @JsonPropertyDescription("Product order number")
  private String poNumber;

  @JsonProperty("invoiceIds")
  @JsonPropertyDescription("List of invoices ID")
  @Valid
  private List<String> invoiceIds = new ArrayList<>();

  @JsonProperty("purchaseOrderId")
  public String getInvoiceId() {
    return purchaseOrderId;
  }


  @JsonProperty("purchaseOrderId")
  public String getPurchaseOrderId() {
    return purchaseOrderId;
  }

  @JsonProperty("purchaseOrderId")
  public void setPurchaseOrderId(String purchaseOrderId) {
    this.purchaseOrderId = purchaseOrderId;
  }

  public InvoiceUpdateDto withId(String purchaseOrderId) {
    this.purchaseOrderId = purchaseOrderId;
    return this;
  }

  @JsonProperty("poNumber")
  public String getPoNumber() {
    return poNumber;
  }

  @JsonProperty("poNumber")
  public void setPoNumber(String poNumber) {
    this.poNumber = poNumber;
  }

  public InvoiceUpdateDto withPoNumber(String poNumber) {
    this.poNumber = poNumber;
    return this;
  }

  @JsonProperty("invoiceIds")
  public List<String> getInvoiceIds() {
    return invoiceIds;
  }

  @JsonProperty("invoiceIds")
  public void setInvoiceIds(List<String> invoiceIds) {
    this.invoiceIds = invoiceIds;
  }

  public InvoiceUpdateDto withInvoiceIds(List<String> invoiceIds) {
    this.invoiceIds = invoiceIds;
    return this;
  }

}
