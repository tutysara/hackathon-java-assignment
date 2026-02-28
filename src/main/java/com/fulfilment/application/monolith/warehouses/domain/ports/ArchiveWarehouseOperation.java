package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

// port
public interface ArchiveWarehouseOperation {
  void archive(Warehouse warehouse);
}
