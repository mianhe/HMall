/**
 * Fulfillment 领域知识 Resource。
 */

export const FULFILLMENT_DOMAIN_URI = 'hmall://fulfillment/domain-knowledge'

export const FULFILLMENT_DOMAIN_KNOWLEDGE = `## 履约（Fulfillment）领域知识

### 生命周期

- 履约单由订单流程自动创建，一个订单可能对应多个履约单（如一个订单同时包含实体商品和虚拟服务，会分别生成 PHYSICAL 和 VIRTUAL 两个履约单）。不支持手动创建或取消履约单（由订单生命周期自动触发）。

### 履约类型

- PHYSICAL（实体履约）：实物商品的物流配送，状态流转：待配货(CREATED) → 配货中(ALLOCATING) → 已发货(SHIPPED) → 已签收(DELIVERED)。
- VIRTUAL（虚拟履约）：服务类商品（碎屏险、延保等）的自动激活，状态流转：待配货(CREATED) → 已激活(ACTIVATED)。虚拟履约由系统在订单创建时自动完成激活，无需人工操作。
- 已取消(CANCELLED)：由取消订单触发，适用于两种类型。

### 实体履约操作

- 配货(allocate)：将待配货的履约单推进到配货中，表示仓库已开始拣货打包。
- 发货(ship)：必须提供物流公司(carrier)和物流单号(trackingNumber)，如「顺丰速运 SF1234567890」。
- 签收(deliver)：确认用户已收到货物，履约完成。

### 查询

- 查询时可按订单号(orderId)或状态(status)筛选。
- status 可选值：CREATED、ALLOCATING、SHIPPED、ACTIVATED、DELIVERED、CANCELLED。
- 返回结果包含履约类型（PHYSICAL/VIRTUAL），方便区分实体物流和虚拟服务。`

export function registerFulfillmentResources(server) {
  server.resource(
    'fulfillment-domain-knowledge',
    FULFILLMENT_DOMAIN_URI,
    { description: '履约领域知识：PHYSICAL/VIRTUAL 两种履约类型、状态流转、操作说明。', mimeType: 'text/plain' },
    async () => ({
      contents: [{ uri: FULFILLMENT_DOMAIN_URI, mimeType: 'text/plain', text: FULFILLMENT_DOMAIN_KNOWLEDGE }],
    }),
  )
}
