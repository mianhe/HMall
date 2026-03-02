package com.hmall.order.application.port;

import java.util.List;

/**
 * 查询某 SPU 可补购的服务列表，由 Catalog 实现。
 */
public interface CatalogServiceQueryPort {

    List<AvailableService> findAvailableServices(Long spuId);

    record AvailableService(Long serviceSkuId, String serviceName, long priceCents) {}
}
