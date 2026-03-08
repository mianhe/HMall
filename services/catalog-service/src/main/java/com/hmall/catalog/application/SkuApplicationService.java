package com.hmall.catalog.application;

import com.hmall.catalog.domain.SpecDimension;
import com.hmall.catalog.domain.SpecDimensionRepository;
import com.hmall.catalog.domain.SpecOption;
import com.hmall.catalog.domain.SpecOptionRepository;
import com.hmall.catalog.domain.SpuRepository;
import com.hmall.catalog.domain.Sku;
import com.hmall.catalog.domain.SkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SKU 应用服务。
 */
@Service
public class SkuApplicationService {

    private final SkuRepository skuRepository;
    private final SpuRepository spuRepository;
    private final SpecOptionRepository optionRepository;
    private final SpecDimensionRepository dimensionRepository;

    public SkuApplicationService(
            SkuRepository skuRepository,
            SpuRepository spuRepository,
            SpecOptionRepository optionRepository,
            SpecDimensionRepository dimensionRepository) {
        this.skuRepository = skuRepository;
        this.spuRepository = spuRepository;
        this.optionRepository = optionRepository;
        this.dimensionRepository = dimensionRepository;
    }

    @Transactional
    public Sku createSku(Long spuId, List<Long> specOptionIds, long priceCents, String displayName) {
        spuRepository.findById(spuId)
            .orElseThrow(() -> new IllegalArgumentException("SPU 不存在"));
        if (priceCents < 0) {
            throw new SkuValidationException("价格不能为负");
        }
        List<Long> optionIds = specOptionIds != null ? specOptionIds : List.of();
        List<SpecDimension> requiredDims = dimensionRepository.findBySpuId(spuId).stream()
            .filter(SpecDimension::isRequired)
            .toList();
        Set<Long> coveredDimensionIds = new HashSet<>();
        for (Long optionId : optionIds) {
            SpecOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new SkuValidationException("Option 不属于该 SPU"));
            SpecDimension dim = dimensionRepository.findById(option.getSpecDimensionId())
                .orElseThrow();
            if (!dim.getSpuId().equals(spuId)) {
                throw new SkuValidationException("Option 不属于该 SPU");
            }
            coveredDimensionIds.add(dim.getId());
        }
        for (SpecDimension dim : requiredDims) {
            if (!coveredDimensionIds.contains(dim.getId())) {
                throw new SkuValidationException("未选齐必填维度：缺少 " + dim.getName());
            }
        }
        Sku sku = new Sku(spuId, displayName, priceCents, optionIds);
        return skuRepository.save(sku);
    }

    @Transactional(readOnly = true)
    public Sku getById(Long skuId) {
        return skuRepository.findById(skuId)
            .orElseThrow(() -> new IllegalArgumentException("SKU 不存在"));
    }

    @Transactional(readOnly = true)
    public List<Sku> findBySpuId(Long spuId) {
        spuRepository.findById(spuId)
            .orElseThrow(() -> new IllegalArgumentException("SPU 不存在"));
        return skuRepository.findBySpuId(spuId);
    }

    @Transactional
    public Sku updatePrice(Long spuId, Long skuId, long priceCents) {
        Sku sku = getById(skuId);
        if (!sku.getSpuId().equals(spuId)) {
            throw new IllegalArgumentException("SKU 不属于该 SPU");
        }
        if (priceCents < 0) {
            throw new SkuValidationException("价格不能为负");
        }
        Sku updated = new Sku(
            sku.getId(),
            sku.getSpuId(),
            sku.getDisplayName(),
            priceCents,
            sku.getSpecOptionIds()
        );
        return skuRepository.save(updated);
    }

    @Transactional
    public void delete(Long spuId, Long skuId) {
        Sku sku = getById(skuId);
        if (!sku.getSpuId().equals(spuId)) {
            throw new IllegalArgumentException("SKU 不属于该 SPU");
        }
        skuRepository.deleteById(skuId);
    }

    /** 解析 specOptionIds 为 dimensionName + optionValue 列表，供 API 返回 specValues */
    public List<SpecValueView> resolveSpecValues(List<Long> specOptionIds) {
        if (specOptionIds == null || specOptionIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SpecOption> optionMap = optionRepository.findByIdIn(specOptionIds).stream()
            .collect(Collectors.toMap(SpecOption::getId, Function.identity()));
        Set<Long> dimIds = optionMap.values().stream()
            .map(SpecOption::getSpecDimensionId).collect(Collectors.toSet());
        Map<Long, SpecDimension> dimMap = dimensionRepository.findByIdIn(new ArrayList<>(dimIds)).stream()
            .collect(Collectors.toMap(SpecDimension::getId, Function.identity()));
        List<SpecValueView> result = new ArrayList<>();
        for (Long optionId : specOptionIds) {
            SpecOption option = optionMap.get(optionId);
            if (option == null) continue;
            SpecDimension dim = dimMap.get(option.getSpecDimensionId());
            if (dim == null) continue;
            result.add(new SpecValueView(dim.getName(), option.getOptionValue()));
        }
        return result;
    }

    public record SpecValueView(String dimensionName, String optionValue) {}
}
