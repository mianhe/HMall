package com.hmall.catalog.application;

import com.hmall.catalog.domain.ProductImage;
import com.hmall.catalog.domain.ProductImageRepository;
import com.hmall.catalog.domain.SpecDimension;
import com.hmall.catalog.domain.SpecDimensionRepository;
import com.hmall.catalog.domain.SpecOption;
import com.hmall.catalog.domain.SpecOptionRepository;
import com.hmall.catalog.domain.SpuRepository;
import com.hmall.catalog.domain.SkuRepository;
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
    private final ProductImageRepository productImageRepository;
    private final SpuRepository spuRepository;
    private final SkuRepository skuRepository;

    public SpecDimensionApplicationService(
            SpecDimensionRepository dimensionRepository,
            SpecOptionRepository optionRepository,
            ProductImageRepository productImageRepository,
            SpuRepository spuRepository,
            SkuRepository skuRepository) {
        this.dimensionRepository = dimensionRepository;
        this.optionRepository = optionRepository;
        this.productImageRepository = productImageRepository;
        this.spuRepository = spuRepository;
        this.skuRepository = skuRepository;
    }

    @Transactional
    public SpecDimension createDimension(Long spuId, String name, boolean required, Integer sortOrder) {
        spuRepository.findById(spuId)
            .orElseThrow(() -> new IllegalArgumentException("SPU 不存在"));
        if (dimensionRepository.existsBySpuIdAndName(spuId, name)) {
            throw new DuplicateSpecDimensionNameException("同 SPU 内维度名称唯一");
        }
        SpecDimension dimension = new SpecDimension(spuId, name, required, sortOrder);
        return dimensionRepository.save(dimension);
    }

    @Transactional
    public SpecOption createOption(Long spuId, Long dimensionId, String optionValue, Integer sortOrder) {
        SpecDimension dimension = dimensionRepository.findById(dimensionId)
            .orElseThrow(() -> new IllegalArgumentException("维度不存在"));
        if (!dimension.getSpuId().equals(spuId)) {
            throw new IllegalArgumentException("维度不存在");
        }
        if (optionRepository.existsBySpecDimensionIdAndOptionValue(dimensionId, optionValue)) {
            throw new DuplicateSpecOptionValueException("同维度内选项值唯一");
        }
        SpecOption option = new SpecOption(dimensionId, optionValue, sortOrder);
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
                List<OptionWithImages> optionsWithImages = options.stream()
                    .map(opt -> {
                        List<ProductImage> images = productImageRepository.findBySpecOptionId(opt.getId());
                        return new OptionWithImages(opt, images);
                    })
                    .toList();
                return new DimensionWithOptions(dim, optionsWithImages);
            })
            .toList();
    }

    // ---------- 展示图 ----------

    @Transactional
    public ProductImage addOptionImage(Long spuId, Long dimensionId, Long optionId, String imageUrl, Integer sortOrder) {
        SpecDimension dimension = dimensionRepository.findById(dimensionId)
            .orElseThrow(() -> new IllegalArgumentException("维度不存在"));
        if (!dimension.getSpuId().equals(spuId)) {
            throw new IllegalArgumentException("维度不存在");
        }
        SpecOption option = optionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("选项不存在"));
        if (!option.getSpecDimensionId().equals(dimensionId)) {
            throw new IllegalArgumentException("选项不存在");
        }
        ProductImage image = ProductImage.optionLevel(spuId, optionId, imageUrl, sortOrder);
        return productImageRepository.save(image);
    }

    @Transactional(readOnly = true)
    public List<ProductImage> listOptionImages(Long spuId, Long dimensionId, Long optionId) {
        SpecDimension dimension = dimensionRepository.findById(dimensionId)
            .orElseThrow(() -> new IllegalArgumentException("维度不存在"));
        if (!dimension.getSpuId().equals(spuId)) {
            throw new IllegalArgumentException("维度不存在");
        }
        SpecOption option = optionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("选项不存在"));
        if (!option.getSpecDimensionId().equals(dimensionId)) {
            throw new IllegalArgumentException("选项不存在");
        }
        return productImageRepository.findBySpecOptionId(optionId);
    }

    @Transactional
    public void deleteOptionImage(Long imageId) {
        productImageRepository.findById(imageId)
            .orElseThrow(() -> new IllegalArgumentException("展示图不存在"));
        productImageRepository.deleteById(imageId);
    }

    @Transactional
    public ProductImage addProductImage(Long spuId, String imageUrl, Integer sortOrder) {
        spuRepository.findById(spuId)
            .orElseThrow(() -> new IllegalArgumentException("SPU 不存在"));
        ProductImage image = ProductImage.productLevel(spuId, imageUrl, sortOrder);
        return productImageRepository.save(image);
    }

    @Transactional(readOnly = true)
    public List<ProductImage> listProductImages(Long spuId) {
        spuRepository.findById(spuId)
            .orElseThrow(() -> new IllegalArgumentException("SPU 不存在"));
        return productImageRepository.findBySpuIdAndSpecOptionIdIsNull(spuId);
    }

    @Transactional
    public void deleteProductImage(Long imageId) {
        productImageRepository.findById(imageId)
            .orElseThrow(() -> new IllegalArgumentException("展示图不存在"));
        productImageRepository.deleteById(imageId);
    }

    @Transactional
    public void deleteOption(Long spuId, Long dimensionId, Long optionId) {
        SpecOption option = optionRepository.findById(optionId)
            .orElseThrow(() -> new IllegalArgumentException("选项不存在"));
        SpecDimension dimension = dimensionRepository.findById(dimensionId)
            .orElseThrow(() -> new IllegalArgumentException("维度不存在"));
        if (!dimension.getSpuId().equals(spuId) || !option.getSpecDimensionId().equals(dimensionId)) {
            throw new IllegalArgumentException("选项不存在");
        }
        if (skuRepository.existsBySpecOptionId(optionId)) {
            throw new SpecOptionInUseException("该选项已被 SKU 使用，无法删除");
        }
        optionRepository.deleteById(optionId);
    }

    /** 维度及其下选项（含展示图）列表 */
    public record DimensionWithOptions(SpecDimension dimension, List<OptionWithImages> optionsWithImages) {}

    /** 选项及其展示图列表 */
    public record OptionWithImages(SpecOption option, List<ProductImage> images) {}
}
