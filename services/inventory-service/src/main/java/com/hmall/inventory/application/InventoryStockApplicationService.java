package com.hmall.inventory.application;

import com.hmall.inventory.api.dto.StockResponseDto;
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
    public List<StockResponseDto> listAll() {
        return skuStockRepository.findAll().stream()
            .map(s -> new StockResponseDto(s.getSkuId(), s.getAvailable(), s.getReserved()))
            .toList();
    }

    @Transactional(readOnly = true)
    public StockResponseDto getBySkuId(Long skuId) {
        SkuStock stock = skuStockRepository.findBySkuId(skuId)
            .orElseThrow(() -> new StockNotFoundException("skuId=" + skuId));
        return new StockResponseDto(stock.getSkuId(), stock.getAvailable(), stock.getReserved());
    }

    @Transactional
    public StockResponseDto setAvailable(Long skuId, int available) {
        if (available < 0) {
            throw new InventoryBadRequestException("available 不能为负");
        }
        SkuStock stock = skuStockRepository.findBySkuId(skuId)
            .orElseGet(() -> new SkuStock(skuId, 0, 0));
        stock.setAvailable(available);
        SkuStock saved = skuStockRepository.save(stock);
        return new StockResponseDto(saved.getSkuId(), saved.getAvailable(), saved.getReserved());
    }
}
