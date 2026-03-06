package com.hmall.catalog.application;

import com.hmall.catalog.domain.EngravingPattern;
import com.hmall.catalog.domain.EngravingPatternRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EngravingPatternApplicationService {

    private final EngravingPatternRepository repository;

    public EngravingPatternApplicationService(EngravingPatternRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EngravingPattern create(String name, String imageUrl, Integer sortOrder, Boolean enabled) {
        EngravingPattern pattern = new EngravingPattern(name, imageUrl, sortOrder, enabled);
        return repository.save(pattern);
    }

    @Transactional
    public EngravingPattern update(Long id, String name, String imageUrl, Integer sortOrder, Boolean enabled) {
        EngravingPattern pattern = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("镭雕图案不存在"));
        pattern.update(name, imageUrl, sortOrder, enabled);
        return repository.save(pattern);
    }

    @Transactional
    public void delete(Long id) {
        repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("镭雕图案不存在"));
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<EngravingPattern> list(Boolean enabledFilter) {
        return repository.findAllOrderBySortOrder(enabledFilter);
    }

    @Transactional(readOnly = true)
    public EngravingPattern getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("镭雕图案不存在"));
    }
}
