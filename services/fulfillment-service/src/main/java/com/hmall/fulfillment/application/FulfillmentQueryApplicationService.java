package com.hmall.fulfillment.application;

import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import com.hmall.fulfillment.domain.FulfillmentOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

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

    /**
     * 列表查询，供管理端使用。orderId 为空时返回全部；status 为空时不过滤状态。
     */
    @Transactional(readOnly = true)
    public List<FulfillmentOrder> list(Long orderId, String status) {
        Stream<FulfillmentOrder> stream = orderId != null
            ? repository.findByOrderId(orderId).stream()
            : repository.findAll().stream();
        if (status != null && !status.isBlank()) {
            FulfillmentOrderStatus s;
            try {
                s = FulfillmentOrderStatus.valueOf(status.trim());
            } catch (IllegalArgumentException e) {
                return List.of();
            }
            stream = stream.filter(o -> o.getStatus() == s);
        }
        return stream.toList();
    }
}
