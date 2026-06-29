package org.folio.service.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxEventFields {

  EVENT_ID("event_id"),
  ENTITY_TYPE("entity_type"),
  ACTION("action"),
  PAYLOAD("payload");

  private final String name;

}
