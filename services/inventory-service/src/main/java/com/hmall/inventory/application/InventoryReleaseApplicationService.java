package com.hmall.inventory.application;

import com.hmall.inventory.api.dto.ReleaseResponseDto;
import com.hmall.inventory.domain.DomainEventPublisher;
import com.hmall.inventory.domain.Reservation;
import com.hmall.inventory.domain.ReservationRepository;
import com.hmall.inventory.domain.ReservationStatus;
import com.hmall.inventory.domain.SkuStock;
import com.hmall.inventory.domain.SkuStockRepository;
import com.hmall.inventory.domain.StockReleased;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class InventoryReleaseApplicationService {

    private final SkuStockRepository skuStockRepository;
    private final ReservationRepository reservationRepository;
    private final DomainEventPublisher eventPublisher;

    public InventoryReleaseApplicationService(SkuStockRepository skuStockRepository,
                                              ReservationRepository reservationRepository,
                                              DomainEventPublisher eventPublisher) {
        this.skuStockRepository = skuStockRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ReleaseResponseDto release(Long orderId) {
        if (orderId == null) {
            throw new InventoryBadRequestException("orderId 不能为空");
        }

        List<Reservation> reserved = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        if (reserved.isEmpty()) {
            return ReleaseResponseDto.ok();
        }

        for (Reservation r : reserved) {
            SkuStock stock = skuStockRepository.findBySkuId(r.getSkuId())
                .orElseThrow(() -> new IllegalStateException("SkuStock not found: " + r.getSkuId()));
            stock.release(r.getQuantity());
            skuStockRepository.save(stock);
            r.markReleased();
            reservationRepository.save(r);
        }

        eventPublisher.publish(new StockReleased(orderId, Instant.now()));
        return ReleaseResponseDto.ok();
    }
}
