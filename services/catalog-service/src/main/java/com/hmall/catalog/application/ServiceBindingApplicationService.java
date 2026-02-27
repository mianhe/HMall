package com.hmall.catalog.application;

import com.hmall.catalog.domain.ServiceBinding;
import com.hmall.catalog.domain.ServiceBindingRepository;
import com.hmall.catalog.domain.Sku;
import com.hmall.catalog.domain.SkuRepository;
import com.hmall.catalog.domain.Spu;
import com.hmall.catalog.domain.SpuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServiceBindingApplicationService {

    private final ServiceBindingRepository bindingRepository;
    private final SkuRepository skuRepository;
    private final SpuRepository spuRepository;

    public ServiceBindingApplicationService(ServiceBindingRepository bindingRepository,
                                            SkuRepository skuRepository,
                                            SpuRepository spuRepository) {
        this.bindingRepository = bindingRepository;
        this.skuRepository = skuRepository;
        this.spuRepository = spuRepository;
    }

    @Transactional
    public ServiceBinding create(Long serviceSkuId, Long targetSpuId, Long priceCents) {
        Sku sku = skuRepository.findById(serviceSkuId)
            .orElseThrow(() -> new IllegalArgumentException("服务 SKU 不存在"));
        Spu serviceSpu = spuRepository.findById(sku.getSpuId())
            .orElseThrow(() -> new IllegalArgumentException("服务商品不存在"));
        if (!"SERVICE".equals(serviceSpu.getProductType())) {
            throw new ServiceBindingBadRequestException("仅 SERVICE 类型商品的 SKU 可创建 ServiceBinding");
        }
        spuRepository.findById(targetSpuId)
            .orElseThrow(() -> new IllegalArgumentException("目标商品不存在"));
        ServiceBinding binding = new ServiceBinding(serviceSkuId, targetSpuId, priceCents);
        return bindingRepository.save(binding);
    }

    @Transactional
    public void delete(Long serviceSkuId, Long bindingId) {
        ServiceBinding binding = bindingRepository.findById(bindingId)
            .orElseThrow(() -> new IllegalArgumentException("ServiceBinding 不存在"));
        if (!binding.getServiceSkuId().equals(serviceSkuId)) {
            throw new IllegalArgumentException("ServiceBinding 不属于该服务 SKU");
        }
        bindingRepository.deleteById(bindingId);
    }

    /**
     * 查询目标实体 SPU 的可选服务，按服务 SPU 分组返回。
     * 每个 ServiceSpuView 包含服务 SPU 信息和其下绑定列表（含 SKU 信息）。
     */
    @Transactional(readOnly = true)
    public List<ServiceSpuView> findAvailableServices(Long targetSpuId) {
        List<ServiceBinding> bindings = bindingRepository.findByTargetSpuId(targetSpuId);

        Map<Long, List<BindingWithSku>> bySpuId = new LinkedHashMap<>();
        for (ServiceBinding b : bindings) {
            Sku sku = skuRepository.findById(b.getServiceSkuId()).orElse(null);
            if (sku == null) continue;
            bySpuId.computeIfAbsent(sku.getSpuId(), k -> new ArrayList<>())
                .add(new BindingWithSku(b, sku));
        }

        List<ServiceSpuView> result = new ArrayList<>();
        for (Map.Entry<Long, List<BindingWithSku>> entry : bySpuId.entrySet()) {
            Spu serviceSpu = spuRepository.findById(entry.getKey()).orElse(null);
            if (serviceSpu == null) continue;
            result.add(new ServiceSpuView(serviceSpu, entry.getValue()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<BindingWithTarget> findByServiceSkuId(Long serviceSkuId) {
        List<ServiceBinding> bindings = bindingRepository.findByServiceSkuId(serviceSkuId);
        return bindings.stream().map(b -> {
            String targetName = spuRepository.findById(b.getTargetSpuId())
                .map(Spu::getName).orElse(null);
            return new BindingWithTarget(b, targetName);
        }).toList();
    }

    public record BindingWithSku(ServiceBinding binding, Sku sku) {}
    public record BindingWithTarget(ServiceBinding binding, String targetSpuName) {}
    public record ServiceSpuView(Spu spu, List<BindingWithSku> bindings) {}
}
