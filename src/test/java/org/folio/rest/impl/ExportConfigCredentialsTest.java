package org.folio.rest.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;

import org.folio.rest.jaxrs.model.Credentials;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ExportConfigCredentialsTest extends TestBase {
  private final Logger logger = LoggerFactory.getLogger(ExportConfigCredentialsTest.class);

  private static final String PASSWORD_FIELD = "password";
  private static final String MY_NEW_PASSWORD = "my_new_password";

  private static final String BATCH_GROUP_ID = "cd592659-77aa-4eb3-ac34-c9a4657bb20f";
  private static final String ANOTHER_BATCH_GROUP_ID = "20780f40-f2e5-4178-9918-107bc461a516";
  
  private static final String BATCH_VOUCHER_EXPORT_CONFIG_ID = "26a4d92b-18ca-4be3-854e-4fb7db03c7a7";
  private static final String ANOTHER_BATCH_VOUCHER_EXPORT_CONFIG_ID = "6544eb9d-a1e4-4d81-a500-b299c8b76068";

  private static final String BATCH_GROUPS_ENDPOINT = TestEntities.BATCH_GROUP.getEndpoint();
  private static final String BATCH_GROUPS_ENDPOINT_WITH_ID = TestEntities.BATCH_GROUP.getEndpointWithId();
  
  private static final String BATCH_VOUCHER_EXPORT_CONFIGS_ENDPOINT = TestEntities.BATCH_VOUCHER_EXPORT_CONFIGS.getEndpoint();
  private static final String BATCH_VOUCHER_EXPORT_CONFIGS_ENDPOINT_WITH_ID = TestEntities.BATCH_VOUCHER_EXPORT_CONFIGS.getEndpointWithId();
  private static final String BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_PARAM = "/batch-voucher-storage/export-configurations/%s/credentials";
  private static final String BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID = "/batch-voucher-storage/export-configurations/{id}/credentials";

  private static final String BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT = String.format(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_PARAM, BATCH_VOUCHER_EXPORT_CONFIG_ID);
  private static final String ANOTHER_BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT = String.format(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_PARAM, ANOTHER_BATCH_VOUCHER_EXPORT_CONFIG_ID);

  private static final String SAMPLE_BATCH_GROUPS_FILE_1 = "data/batch-voucher-export-configs/batch-groups/test_group.json";
  private static final String SAMPLE_BATCH_GROUPS_FILE_2 = "data/batch-voucher-export-configs/batch-groups/test_group2.json";
  private static final String SAMPLE_BATCH_VOUCHER_EXPORT_CONFIGS_FILE_1 = "data/batch-voucher-export-configs/test_config.json";
  private static final String SAMPLE_BATCH_VOUCHER_EXPORT_CONFIGS_FILE_2 = "data/batch-voucher-export-configs/test_config2.json";
  private static final String SAMPLE_CREDENTIALS_FILE_1 = "data/batch-voucher-export-configs/credentials/test_config_credentials.json";
  private static final String SAMPLE_CREDENTIALS_FILE_2 = "data/batch-voucher-export-configs/credentials/test_config2_credentials.json";

  private static final String simpleClassName = Credentials.class.getSimpleName();

  @Test
  public void testExportConfigCredentialsCrud() throws MalformedURLException {
    try {
      logger.info(String.format("--- mod-invoice-storage %s test: Creating %s ... ", simpleClassName, simpleClassName));

      // prepare batch groups
      String batchGroupSample1 = getFile(SAMPLE_BATCH_GROUPS_FILE_1);
      postData(BATCH_GROUPS_ENDPOINT, batchGroupSample1).then().statusCode(201);
      
      // prepare batch voucher export configs
      String exportConfigSample1 = getFile(SAMPLE_BATCH_VOUCHER_EXPORT_CONFIGS_FILE_1);
      postData(BATCH_VOUCHER_EXPORT_CONFIGS_ENDPOINT, exportConfigSample1).then().statusCode(201);

      String sample = getFile(SAMPLE_CREDENTIALS_FILE_1);
      Response response = postData(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT, sample);

      logger.info(String.format("--- mod-invoice-storage %s test: Valid fields exists ... ", simpleClassName));
      JsonObject sampleJson = JsonObject.mapFrom(new JsonObject(sample).mapTo(Credentials.class));

      JsonObject responseJson = JsonObject.mapFrom(response.then().extract().response().as(Credentials.class));
      testAllFieldsExists(responseJson, sampleJson);

      logger.info(String.format("--- mod-invoice-storage %s test: Fetching %s with ID: %s", simpleClassName, simpleClassName, BATCH_VOUCHER_EXPORT_CONFIG_ID));
      Credentials credentials = getData(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT).then()
        .log().ifValidationFails()
        .statusCode(200).log().ifValidationFails()
        .extract()
        .body().as(Credentials.class);

      assertEquals(BATCH_VOUCHER_EXPORT_CONFIG_ID, credentials.getExportConfigId());

      logger.info(String.format("--- mod-invoice-storage %s test: Editing %s with ID: %s", simpleClassName, simpleClassName, BATCH_VOUCHER_EXPORT_CONFIG_ID));
      credentials.setPassword(MY_NEW_PASSWORD);
      JsonObject catJSON = JsonObject.mapFrom(credentials);
      testEntityEdit(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID, catJSON.toString(), BATCH_VOUCHER_EXPORT_CONFIG_ID);

      logger.info(String.format("--- mod-invoice-storage %s test: Fetching updated %s with ID: %s", simpleClassName, simpleClassName, BATCH_VOUCHER_EXPORT_CONFIG_ID));
      String existedValue = getData(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT)
        .then()
        .log().ifValidationFails()
        .statusCode(200).log().ifValidationFails()
          .extract()
            .body()
              .jsonPath()
              .get(PASSWORD_FIELD).toString();
      assertEquals(MY_NEW_PASSWORD, existedValue);

    } catch (Exception e) {
      logger.error(String.format("--- mod-invoice-storage-test: %s API ERROR: %s", simpleClassName, e.getMessage()));
      fail(e.getMessage());
    } finally {
      logger.info(String.format("--- mod-invoice-storages %s test: Deleting %s with ID: %s", simpleClassName, simpleClassName, BATCH_VOUCHER_EXPORT_CONFIG_ID));
      deleteDataSuccess(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID, BATCH_VOUCHER_EXPORT_CONFIG_ID);

      logger.info(String.format("--- mod-invoice-storages %s test: Verify %s is deleted with ID: %s", simpleClassName, simpleClassName, BATCH_VOUCHER_EXPORT_CONFIG_ID));
      testVerifyEntityDeletion(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID, BATCH_VOUCHER_EXPORT_CONFIG_ID);

      deleteDataSuccess(BATCH_VOUCHER_EXPORT_CONFIGS_ENDPOINT_WITH_ID, BATCH_VOUCHER_EXPORT_CONFIG_ID);
      testVerifyEntityDeletion(BATCH_VOUCHER_EXPORT_CONFIGS_ENDPOINT_WITH_ID, BATCH_VOUCHER_EXPORT_CONFIG_ID);
      
      deleteDataSuccess(BATCH_GROUPS_ENDPOINT_WITH_ID, BATCH_GROUP_ID);
      testVerifyEntityDeletion(BATCH_GROUPS_ENDPOINT_WITH_ID, BATCH_GROUP_ID);
    }
  }

  @Test
  public void testFetchEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s get by id test: Invalid %s: %s", simpleClassName,simpleClassName, NON_EXISTED_ID));
    getDataById(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID, NON_EXISTED_ID).then().log().ifValidationFails()
      .statusCode(404);
  }

  @Test
  public void testEditEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s put by id test: Invalid %s: %s", simpleClassName, simpleClassName, NON_EXISTED_ID));
    String sampleData = getFile(SAMPLE_CREDENTIALS_FILE_1);
    Credentials credentials = (new JsonObject(sampleData).mapTo(Credentials.class));
    credentials.setId(NON_EXISTED_ID);
    credentials.setExportConfigId(NON_EXISTED_ID);
    sampleData = JsonObject.mapFrom(credentials).encode();
    putData(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID, NON_EXISTED_ID, sampleData)
      .then().log().ifValidationFails()
      .statusCode(404);
  }

  @Test
  public void testDeleteEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s delete by id test: Invalid %s: %s", simpleClassName, simpleClassName, NON_EXISTED_ID));
    deleteData(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID, NON_EXISTED_ID)
      .then().log().ifValidationFails()
      .statusCode(404);
  }

  @Test
  public void testEntityWithMismatchId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s put by id test: Invalid %s: %s", simpleClassName, simpleClassName, NON_EXISTED_ID));

    // prepare batch groups
    String batchGroupSample1 = getFile(SAMPLE_BATCH_GROUPS_FILE_1);
    postData(BATCH_GROUPS_ENDPOINT, batchGroupSample1).then().statusCode(201);
    String batchGroupSample2 = getFile(SAMPLE_BATCH_GROUPS_FILE_2);
    postData(BATCH_GROUPS_ENDPOINT, batchGroupSample2).then().statusCode(201);
    
    // prepare batch voucher export config data
    String exportConfigSample1 = getFile(SAMPLE_BATCH_VOUCHER_EXPORT_CONFIGS_FILE_1);
    postData(BATCH_VOUCHER_EXPORT_CONFIGS_ENDPOINT, exportConfigSample1).then().statusCode(201);
    String exportConfigSample2 = getFile(SAMPLE_BATCH_VOUCHER_EXPORT_CONFIGS_FILE_2);
    postData(BATCH_VOUCHER_EXPORT_CONFIGS_ENDPOINT, exportConfigSample2).then().statusCode(201);

    String sampleCredential_1 = getFile(SAMPLE_CREDENTIALS_FILE_1);
    // create batch voucher export config credentials with id = BATCH_VOUCHER_EXPORT_CONFIG_ID
    postData(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT, sampleCredential_1).then().statusCode(201);

    String sampleCredential_2 = getFile(SAMPLE_CREDENTIALS_FILE_2);
    // create batch voucher export config credentials with id = ANOTHER_BATCH_VOUCHER_EXPORT_CONFIG_ID
    postData(ANOTHER_BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT, sampleCredential_2).then().statusCode(201);

    // try to create batch voucher export config credentials with mismatched id
    postData(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT, sampleCredential_2).then().statusCode(400);

    // update batch voucher export config credentials with mismatched id
    putData(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID, BATCH_VOUCHER_EXPORT_CONFIG_ID, sampleCredential_2)
      .then().log().ifValidationFails()
      .statusCode(400);

    // batch voucher export config credentials cleanup
    deleteDataSuccess(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID, BATCH_VOUCHER_EXPORT_CONFIG_ID);
    deleteDataSuccess(BATCH_VOUCHER_EXPORT_CONFIG_CREDENTIALS_ENDPOINT_WITH_ID, ANOTHER_BATCH_VOUCHER_EXPORT_CONFIG_ID);

    // batch voucher export config cleanup
    deleteDataSuccess(BATCH_VOUCHER_EXPORT_CONFIGS_ENDPOINT_WITH_ID, BATCH_VOUCHER_EXPORT_CONFIG_ID);
    deleteDataSuccess(BATCH_VOUCHER_EXPORT_CONFIGS_ENDPOINT_WITH_ID, ANOTHER_BATCH_VOUCHER_EXPORT_CONFIG_ID);
    
    // batch group cleanup
    deleteDataSuccess(BATCH_GROUPS_ENDPOINT_WITH_ID, BATCH_GROUP_ID);
    deleteDataSuccess(BATCH_GROUPS_ENDPOINT_WITH_ID, ANOTHER_BATCH_GROUP_ID);
  }
}