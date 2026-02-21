package com.hmall.fulfillment.domain;

import java.util.List;
import java.util.Optional;

public interface FulfillmentOrderRepository {

    FulfillmentOrder save(FulfillmentOrder order);

    Optional<FulfillmentOrder> findById(Long fulfillmentOrderId);

    List<FulfillmentOrder> findByOrderId(Long orderId);
}
