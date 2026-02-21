package com.hmall.fulfillment.application;

import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import com.hmall.fulfillment.domain.FulfillmentOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FulfillmentCancelApplicationService {

    private final FulfillmentOrderRepository repository;

    public FulfillmentCancelApplicationService(FulfillmentOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CancelResult cancel(Long orderId) {
        if (orderId == null) {
            throw new FulfillmentBadRequestException("orderId 不能为空");
        }

        List<FulfillmentOrder> orders = repository.findByOrderId(orderId);
        int cancelledCount = 0;
        for (FulfillmentOrder order : orders) {
            if (order.getStatus() == FulfillmentOrderStatus.CREATED) {
                order.cancel();
                repository.save(order);
                cancelledCount++;
            }
        }
        return new CancelResult(orderId, cancelledCount);
    }

    public record CancelResult(Long orderId, int cancelledCount) {}
}
