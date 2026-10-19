package org.folio.service.audit;

public record AuditEntityWrapper<T, P>(T entity, T originalEntity, P parentEntity) {
}
