package com.smbtech.examples.openapicontract;

import com.smbtech.contracts.warehouseinventorycatalog.v1.api.DefaultApiDelegate;
import com.smbtech.contracts.warehouseinventorycatalog.v1.model.InventoryItemResponse;
import com.smbtech.contracts.warehouseinventorycatalog.v1.model.InventoryStatus;
import com.smbtech.contracts.warehouseinventorycatalog.v1.model.WarehouseSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
final class WarehouseInventoryDelegate implements DefaultApiDelegate {

    @Override
    public ResponseEntity<InventoryItemResponse> getWarehouseInventoryItem(
            String warehouseId, String sku) {
        InventoryItemResponse response =
                new InventoryItemResponse(
                                sku,
                                InventoryStatus.AVAILABLE,
                                42L,
                                new WarehouseSummary(warehouseId, "CL"))
                        .addTagsItem("available");
        return ResponseEntity.ok(response);
    }
}
