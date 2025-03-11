# mod-invoice-storage

Copyright (C) 2019-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

This is the Invoice storage module.

*NOTE*: This module requires postgresql v10 or later
 
## Additional information

### Kafka domain event pattern
The pattern means that every time when a domain entity is created/updated a message is posted to kafka topic.
Currently, domain events are supported for orders, order lines and pieces The events are posted into the following topics:

- `ACQ_ORDER_CHANGED` - for orders
- `ACQ_ORDER_LINE_CHANGED` - for order lines
- `ACQ_PIECE_CHANGED` - for pieces

The event payload has the following structure:
```json5
{
  "id": "12bb13f6-d0fa-41b5-b0ad-d6561975121b",
  "action": "CREATED|UPDATED|DELETED",
  "userId": "1d4f3f6-d0fa-41b5-b0ad-d6561975121b",
  "eventDate": "2024-11-14T10:00:00.000+0000",
  "actionDate": "2024-11-14T10:00:00.000+0000",
  "entitySnapshot": { } // entity being either: order, orderLine, piece
}
```

Default value for all partitions is 1.
Kafka partition key for all the events is entity id.

### Issue tracker

See project [MODINVOSTO](https://issues.folio.org/browse/MODINVOSTO)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at
[dev.folio.org](https://dev.folio.org/)
