package com.hmall.catalog.application;

import com.hmall.catalog.domain.SpecDimension;
import com.hmall.catalog.domain.SpecDimensionRepository;
import com.hmall.catalog.domain.SpecOption;
import com.hmall.catalog.domain.SpecOptionRepository;
import com.hmall.catalog.domain.SpuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 规格维度与选项应用服务。
 */
@Service
public class SpecDimensionApplicationService {

    private final SpecDimensionRepository dimensionRepository;
    private final SpecOptionRepository optionRepository;
    private final SpuRepository spuRepository;

    public SpecDimensionApplicationService(
            SpecDimensionRepository dimensionRepository,
            SpecOptionRepository optionRepository,
            SpuRepository spuRepository) {
        this.dimensionRepository = dimensionRepository;
        this.optionRepository = optionRepository;
        this.spuRepository = spuRepository;
    }

    @Transactional
    public SpecDimension createDimension(Long spuId, String name, boolean required, Integer sortOrder, boolean affectsAppearance) {
        spuRepository.findById(spuId)
            .orElseThrow(() -> new IllegalArgumentException("SPU 不存在"));
        if (dimensionRepository.existsBySpuIdAndName(spuId, name)) {
            throw new DuplicateSpecDimensionNameException("同 SPU 内维度名称唯一");
        }
        SpecDimension dimension = new SpecDimension(spuId, name, required, sortOrder, affectsAppearance);
        return dimensionRepository.save(dimension);
    }

    @Transactional
    public SpecOption createOption(Long spuId, Long dimensionId, String optionValue, Integer sortOrder, String image) {
        SpecDimension dimension = dimensionRepository.findById(dimensionId)
            .orElseThrow(() -> new IllegalArgumentException("维度不存在"));
        if (!dimension.getSpuId().equals(spuId)) {
            throw new IllegalArgumentException("维度不存在");
        }
        if (optionRepository.existsBySpecDimensionIdAndOptionValue(dimensionId, optionValue)) {
            throw new DuplicateSpecOptionValueException("同维度内选项值唯一");
        }
        SpecOption option = new SpecOption(dimensionId, optionValue, sortOrder, image);
        return optionRepository.save(option);
    }

    @Transactional(readOnly = true)
    public List<DimensionWithOptions> listDimensionsWithOptions(Long spuId) {
        spuRepository.findById(spuId)
            .orElseThrow(() -> new IllegalArgumentException("SPU 不存在"));
        List<SpecDimension> dimensions = dimensionRepository.findBySpuId(spuId);
        return dimensions.stream()
            .map(dim -> {
                List<SpecOption> options = optionRepository.findBySpecDimensionId(dim.getId());
                return new DimensionWithOptions(dim, options);
            })
            .toList();
    }

    /** 维度及其下选项列表，供 GET 维度及选项使用 */
    public record DimensionWithOptions(SpecDimension dimension, List<SpecOption> options) {}
}
