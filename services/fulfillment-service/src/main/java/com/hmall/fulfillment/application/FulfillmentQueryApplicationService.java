package com.hmall.fulfillment.application;

import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FulfillmentQueryApplicationService {

    private final FulfillmentOrderRepository repository;

    public FulfillmentQueryApplicationService(FulfillmentOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public FulfillmentOrder getById(Long fulfillmentOrderId) {
        return repository.findById(fulfillmentOrderId)
            .orElseThrow(() -> new FulfillmentNotFoundException("履约单不存在: " + fulfillmentOrderId));
    }

    @Transactional(readOnly = true)
    public List<FulfillmentOrder> listByOrderId(Long orderId) {
        return repository.findByOrderId(orderId);
    }
}
