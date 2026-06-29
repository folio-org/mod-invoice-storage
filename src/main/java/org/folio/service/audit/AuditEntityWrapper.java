package org.folio.service.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntityWrapper<T> {

  private T entity;
  private T originalEntity;

  public static <T> AuditEntityWrapper<T> of(T entity, T originalEntity) {
    return new AuditEntityWrapper<>(entity, originalEntity);
  }
}