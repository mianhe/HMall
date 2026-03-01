/**
 * Inventory 领域知识 Resource。
 */

export const INVENTORY_DOMAIN_URI = 'hmall://inventory/domain-knowledge'

export const INVENTORY_DOMAIN_KNOWLEDGE = `## 库存（Inventory）领域知识

### 数据模型

- 库存以 SKU 为粒度，每个 SKU 有两个水位：
  - available（可用库存）：可手动设置，表示可售数量。
  - reserved（已占用库存）：由订单流程自动维护（下单占用、取消释放），不可手动修改。

### 业务规则

- ⚠️ 仅 PHYSICAL 类型商品需要库存。SERVICE 类型商品（碎屏险、延保等虚拟服务）不需要库存管理——下单时系统自动跳过服务商品的库存占用。不要为 SERVICE SKU 设置库存。
- inventory_stock update 设置可用库存时，若 SKU 库存记录不存在会自动创建。`

export function registerInventoryResources(server) {
  server.resource(
    'inventory-domain-knowledge',
    INVENTORY_DOMAIN_URI,
    { description: '库存领域知识：库存模型（available/reserved）、PHYSICAL 与 SERVICE 的库存规则。', mimeType: 'text/plain' },
    async () => ({
      contents: [{ uri: INVENTORY_DOMAIN_URI, mimeType: 'text/plain', text: INVENTORY_DOMAIN_KNOWLEDGE }],
    }),
  )
}
