/**
 * Cart + Order 领域知识 Resource（面向消费者购物场景）。
 */

export const CART_ORDER_DOMAIN_URI = 'hmall://cart-order/domain-knowledge'

export const CART_ORDER_DOMAIN_KNOWLEDGE = `## 购物车与订单领域知识

### 服务类商品

- 实体商品可附带服务（如"华为 Care+ 一年期"），服务价格可能因所选实体商品而不同。
- 查看某实体商品的可选服务：
  - 方式1：catalog_products get detail=full → 从返回的 available-services 中获取。
  - 方式2：catalog_available_services(spuId) → 返回按服务分组的列表，含最终售价。

### 购物车

- 购物车按用户隔离，每项有 cartItemId。
- ⚠️ 加购实体商品：cart_manage add(skuId, quantity)。
- ⚠️ 加购服务商品：cart_manage add(skuId=服务SKU的ID, quantity, relatedSkuId=关联的实体SKU的ID)。SERVICE 商品加购时 relatedSkuId 必传，用于关联到用户选购的具体实体商品。

### 下单

- 下单需要 items（skuId + quantity）和收货地址（六要素：recipientName、phone、province、city、district、detail）。
- 购物车下单时，skuId 和 quantity 必须用购物车返回的数据，不能替换。
- 下单时优先用 user_addresses list 查询已有收货地址供用户选择，避免每次手动输入。

### 订单管理

- 用户可通过 order_query 查看订单列表和详情，也可取消订单（仅待付款/已付款/配货中状态可取消）。`

export function registerCartOrderResources(server) {
  server.resource(
    'cart-order-domain-knowledge',
    CART_ORDER_DOMAIN_URI,
    { description: '购物车与订单领域知识：服务商品加购规则（relatedSkuId）、下单流程、订单状态。', mimeType: 'text/plain' },
    async () => ({
      contents: [{ uri: CART_ORDER_DOMAIN_URI, mimeType: 'text/plain', text: CART_ORDER_DOMAIN_KNOWLEDGE }],
    }),
  )
}
