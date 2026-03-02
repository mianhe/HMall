package com.hmall.inventory.application;

import com.hmall.inventory.application.port.InventoryEventPublisher;
import com.hmall.inventory.domain.Reservation;
import com.hmall.inventory.domain.ReservationRepository;
import com.hmall.inventory.domain.ReservationStatus;
import com.hmall.inventory.domain.SkuStock;
import com.hmall.inventory.domain.SkuStockRepository;
import com.hmall.inventory.domain.StockReserved;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryOccupyApplicationService {

    private final SkuStockRepository skuStockRepository;
    private final ReservationRepository reservationRepository;
    private final InventoryEventPublisher eventPublisher;

    public InventoryOccupyApplicationService(SkuStockRepository skuStockRepository,
                                             ReservationRepository reservationRepository,
                                             InventoryEventPublisher eventPublisher) {
        this.skuStockRepository = skuStockRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void occupy(Long orderId, List<OccupyItem> items) {
        if (orderId == null) {
            throw new InventoryBadRequestException("orderId 不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new InventoryBadRequestException("items 不能为空");
        }

        if (reservationRepository.existsByOrderIdAndStatus(orderId, ReservationStatus.RESERVED)) {
            return;
        }

        Map<Long, Integer> qtyBySku = new HashMap<>();
        for (OccupyItem item : items) {
            if (item.skuId() == null || item.quantity() <= 0) {
                throw new InventoryBadRequestException("items 中 skuId 与 quantity 必填且 quantity 大于 0");
            }
            qtyBySku.merge(item.skuId(), item.quantity(), Integer::sum);
        }
        List<SkuStock> toSave = new ArrayList<>();
        for (var e : qtyBySku.entrySet()) {
            Long skuId = e.getKey();
            int quantity = e.getValue();
            SkuStock stock = skuStockRepository.findBySkuId(skuId)
                .orElseGet(() -> new SkuStock(skuId, 0, 0));
            if (!stock.occupy(quantity)) {
                throw new InventoryBadRequestException("库存不足：skuId=" + skuId);
            }
            toSave.add(stock);
        }
        for (SkuStock stock : toSave) {
            skuStockRepository.save(stock);
        }

        List<StockReserved.OccupyItemPayload> payloads = new ArrayList<>();
        for (OccupyItem item : items) {
            Reservation reservation = new Reservation(orderId, item.skuId(), item.quantity());
            reservationRepository.save(reservation);
            payloads.add(new StockReserved.OccupyItemPayload(item.skuId(), item.quantity()));
        }

        eventPublisher.publish(new StockReserved(orderId, payloads, Instant.now()));
    }
}
