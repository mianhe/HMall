package com.hmall.catalog.application;

import com.hmall.catalog.domain.CategoryRepository;
import com.hmall.catalog.domain.Spu;
import com.hmall.catalog.domain.SpuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SpuApplicationService {

    private final SpuRepository spuRepository;
    private final CategoryRepository categoryRepository;

    public SpuApplicationService(SpuRepository spuRepository, CategoryRepository categoryRepository) {
        this.spuRepository = spuRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Spu create(Long categoryId, String name, String description, String productType) {
        var category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("类别不存在"));
        if (categoryRepository.existsByParentId(category.getId())) {
            throw new NotLeafCategoryException("仅叶子类别可挂商品");
        }
        Spu spu = new Spu(categoryId, name, description, productType);
        return spuRepository.save(spu);
    }

    @Transactional(readOnly = true)
    public List<Spu> listByCategoryId(Long categoryId) {
        return spuRepository.findByCategoryId(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Spu> searchByName(String keyword) {
        return spuRepository.searchByName(keyword);
    }

    @Transactional(readOnly = true)
    public Spu getById(Long id) {
        return spuRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    }

    @Transactional
    public Spu update(Long id, String name, String description) {
        Spu existing = spuRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        Spu updated = new Spu(existing.getId(), existing.getCategoryId(), name, description,
            existing.getProductType());
        return spuRepository.save(updated);
    }

    @Transactional
    public void delete(Long id) {
        spuRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        spuRepository.deleteById(id);
    }
}
