package org.folio.rest.utils;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.prepareTenant;
import static org.folio.rest.utils.TenantApiTestUtil.purge;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.TenantJob;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.restassured.http.Header;

public class IsolatedTenantExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

  private static final Logger LOGGER = LogManager.getLogger(IsolatedTenantExtension.class);
  private static final String ISOLATED_TENANT = "isolated";
  private TenantJob tenantJob;

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    if (hasTenantAnnotationClassOrMethod(context)) {
      final Header tenantHeader = new Header(OKAPI_HEADER_TENANT, ISOLATED_TENANT);

      tenantJob = prepareTenant(tenantHeader, false, false);
      LOGGER.info("Isolated tenant has been prepared");
    }
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    if (hasTenantAnnotationClassOrMethod(context)) {
      final Header tenantHeader = new Header(OKAPI_HEADER_TENANT, ISOLATED_TENANT);

      deleteTenant(tenantJob, tenantHeader);
      purge(tenantHeader);
      LOGGER.info("Isolated tenant has been deleted");

    }
  }

  private Boolean hasTenantAnnotationClassOrMethod(ExtensionContext context) {
    return
      context.getElement().map(el -> ((Method)el).getDeclaringClass().isAnnotationPresent(IsolatedTenant.class)).orElse(false)
        || context.getElement().map(el -> isAnnotated(el, IsolatedTenant.class)).orElse(false);
  }
}
