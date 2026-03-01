package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.AvailableServiceDto;
import com.hmall.catalog.api.dto.AvailableServiceSkuDto;
import com.hmall.catalog.api.dto.ServiceBindingCreateDto;
import com.hmall.catalog.api.dto.ServiceBindingDto;
import com.hmall.catalog.api.dto.ServiceBindingUpdateDto;
import com.hmall.catalog.api.dto.SkuSpecValueDto;
import com.hmall.catalog.application.ServiceBindingApplicationService;
import com.hmall.catalog.application.ServiceBindingApplicationService.BindingWithSku;
import com.hmall.catalog.application.ServiceBindingApplicationService.BindingWithTarget;
import com.hmall.catalog.application.ServiceBindingApplicationService.ServiceSpuView;
import com.hmall.catalog.application.SkuApplicationService;
import com.hmall.catalog.domain.ServiceBinding;
import com.hmall.catalog.domain.Spu;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ServiceBindingController {

    private final ServiceBindingApplicationService bindingService;
    private final SkuApplicationService skuApplicationService;

    public ServiceBindingController(ServiceBindingApplicationService bindingService,
                                    SkuApplicationService skuApplicationService) {
        this.bindingService = bindingService;
        this.skuApplicationService = skuApplicationService;
    }

    @PostMapping("/api/skus/{skuId}/service-bindings")
    public ResponseEntity<ServiceBindingDto> create(@PathVariable Long skuId,
                                                    @RequestBody ServiceBindingCreateDto dto) {
        ServiceBinding binding = bindingService.create(skuId, dto.targetSpuId(), dto.priceCents());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(binding, null));
    }

    @GetMapping("/api/skus/{skuId}/service-bindings")
    public ResponseEntity<List<ServiceBindingDto>> listBySkuId(@PathVariable Long skuId) {
        List<BindingWithTarget> bindings = bindingService.findByServiceSkuId(skuId);
        List<ServiceBindingDto> result = bindings.stream()
            .map(bt -> toDto(bt.binding(), bt.targetSpuName()))
            .toList();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/api/skus/{skuId}/service-bindings/{bindingId}")
    public ResponseEntity<ServiceBindingDto> update(@PathVariable Long skuId,
                                                    @PathVariable Long bindingId,
                                                    @RequestBody ServiceBindingUpdateDto dto) {
        ServiceBinding updated = bindingService.updatePrice(skuId, bindingId, dto.priceCents());
        return ResponseEntity.ok(toDto(updated, null));
    }

    @DeleteMapping("/api/skus/{skuId}/service-bindings/{bindingId}")
    public ResponseEntity<Void> delete(@PathVariable Long skuId, @PathVariable Long bindingId) {
        bindingService.delete(skuId, bindingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/products/{targetSpuId}/available-services")
    public ResponseEntity<List<AvailableServiceDto>> getAvailableServices(@PathVariable Long targetSpuId) {
        List<ServiceSpuView> views = bindingService.findAvailableServices(targetSpuId);
        List<AvailableServiceDto> result = views.stream().map(this::toAvailableServiceDto).toList();
        return ResponseEntity.ok(result);
    }

    private ServiceBindingDto toDto(ServiceBinding b, String targetSpuName) {
        return new ServiceBindingDto(b.getId(), b.getServiceSkuId(), b.getTargetSpuId(), b.getPriceCents(), targetSpuName);
    }

    private AvailableServiceDto toAvailableServiceDto(ServiceSpuView view) {
        Spu spu = view.spu();
        List<AvailableServiceSkuDto> bindingDtos = view.bindings().stream()
            .map(this::toAvailableServiceSkuDto)
            .toList();
        return new AvailableServiceDto(
            spu.getId(), spu.getName(), spu.getDescription(),
            spu.getProductType(), bindingDtos);
    }

    private AvailableServiceSkuDto toAvailableServiceSkuDto(BindingWithSku bws) {
        ServiceBinding b = bws.binding();
        Long effectivePrice = b.getPriceCents() != null ? b.getPriceCents() : bws.sku().getPriceCents();
        List<SkuSpecValueDto> specValues = skuApplicationService.resolveSpecValues(bws.sku().getSpecOptionIds())
            .stream()
            .map(v -> new SkuSpecValueDto(v.dimensionName(), v.optionValue()))
            .toList();
        return new AvailableServiceSkuDto(b.getId(), b.getServiceSkuId(), effectivePrice, specValues);
    }
}
