package com.hmall.inventory.application;

import com.hmall.inventory.domain.SkuStock;
import com.hmall.inventory.domain.SkuStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryStockApplicationService {

    private final SkuStockRepository skuStockRepository;

    public InventoryStockApplicationService(SkuStockRepository skuStockRepository) {
        this.skuStockRepository = skuStockRepository;
    }

    @Transactional(readOnly = true)
    public List<SkuStock> listAll() {
        return skuStockRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SkuStock getBySkuId(Long skuId) {
        return skuStockRepository.findBySkuId(skuId)
            .orElseThrow(() -> new StockNotFoundException("skuId=" + skuId));
    }

    @Transactional
    public SkuStock setAvailable(Long skuId, int available) {
        if (available < 0) {
            throw new InventoryBadRequestException("available 不能为负");
        }
        SkuStock stock = skuStockRepository.findBySkuId(skuId)
            .orElseGet(() -> new SkuStock(skuId, 0, 0));
        stock.setAvailable(available);
        return skuStockRepository.save(stock);
    }
}
