package com.hmall.inventory.acceptance;

import com.hmall.inventory.domain.DomainEventPublisher;
import com.hmall.inventory.domain.StockReleased;
import com.hmall.inventory.domain.StockReserved;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试用领域事件捕获器，记录发布的 StockReserved、StockReleased 事件。
 */
public class EventCapture implements DomainEventPublisher {

    private final List<StockReserved> stockReservedEvents = new CopyOnWriteArrayList<>();
    private final List<StockReleased> stockReleasedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(StockReserved event) {
        stockReservedEvents.add(event);
    }

    @Override
    public void publish(StockReleased event) {
        stockReleasedEvents.add(event);
    }

    public List<StockReserved> getStockReservedEvents() {
        return new ArrayList<>(stockReservedEvents);
    }

    public List<StockReleased> getStockReleasedEvents() {
        return new ArrayList<>(stockReleasedEvents);
    }

    public void clear() {
        stockReservedEvents.clear();
        stockReleasedEvents.clear();
    }
}
