package com.hmall.inventory.api;

import com.hmall.inventory.api.dto.OccupyRequestDto;
import com.hmall.inventory.api.dto.OccupyResponseDto;
import com.hmall.inventory.api.dto.ReleaseRequestDto;
import com.hmall.inventory.api.dto.ReleaseResponseDto;
import com.hmall.inventory.api.dto.StockResponseDto;
import com.hmall.inventory.api.dto.StockUpdateRequestDto;
import com.hmall.inventory.application.InventoryOccupyApplicationService;
import com.hmall.inventory.application.InventoryReleaseApplicationService;
import com.hmall.inventory.application.InventoryStockApplicationService;
import com.hmall.inventory.application.OccupyItem;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryOccupyApplicationService occupyApplicationService;
    private final InventoryReleaseApplicationService releaseApplicationService;
    private final InventoryStockApplicationService stockApplicationService;

    public InventoryController(InventoryOccupyApplicationService occupyApplicationService,
                               InventoryReleaseApplicationService releaseApplicationService,
                               InventoryStockApplicationService stockApplicationService) {
        this.occupyApplicationService = occupyApplicationService;
        this.releaseApplicationService = releaseApplicationService;
        this.stockApplicationService = stockApplicationService;
    }

    @PostMapping("/occupy")
    public ResponseEntity<OccupyResponseDto> occupy(@Valid @RequestBody OccupyRequestDto dto) {
        OccupyResponseDto result = occupyApplicationService.occupy(dto.orderId(), dto.items().stream()
            .map(item -> new OccupyItem(item.skuId(), item.quantity()))
            .toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/release")
    public ResponseEntity<ReleaseResponseDto> release(@Valid @RequestBody ReleaseRequestDto dto) {
        ReleaseResponseDto result = releaseApplicationService.release(dto.orderId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stock/{skuId}")
    public ResponseEntity<StockResponseDto> getStock(@PathVariable Long skuId) {
        StockResponseDto result = stockApplicationService.getBySkuId(skuId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/stock/{skuId}")
    public ResponseEntity<StockResponseDto> setStock(
            @PathVariable Long skuId,
            @Valid @RequestBody StockUpdateRequestDto dto) {
        StockResponseDto result = stockApplicationService.setAvailable(skuId, dto.available());
        return ResponseEntity.ok(result);
    }
}
