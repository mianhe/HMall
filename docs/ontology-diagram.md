# HMall 电商本体 — 对象关系图

> 对象类型与关联关系的可视化表达。文本版本见 [ontology.md](./ontology.md)。

## 全景图

```mermaid
classDiagram
    direction LR

    namespace Catalog {
        class Category {
            <<聚合根>>
            categoryId
            parentId
            name
        }
        class Spu {
            <<聚合根>>
            spuId
            productType: PHYSICAL | SERVICE
            name
        }
        class SpecDimension {
            <<实体>>
            specDimensionId
            name
        }
        class SpecOption {
            <<实体>>
            specOptionId
            optionValue
        }
        class Sku {
            <<实体>>
            skuId ⟵ 全局唯一
            priceCents
        }
        class ServiceBinding {
            <<实体>>
            serviceBindingId
            priceCents?
        }
        class EngravingPattern {
            <<聚合根>>
            patternId
            name
            imageUrl
        }
    }

    namespace User {
        class User_User {
            <<聚合根>>
            userId
            username
        }
        class Address {
            <<实体>>
            addressId
            收件人 / 电话 / 省市区 / 详细地址
        }
    }

    namespace Cart {
        class Cart_Cart {
            <<聚合根>>
            cartId
            userId
        }
        class CartItem {
            <<实体>>
            cartItemId
            skuId
            relatedSkuId?
            quantity
        }
    }

    namespace Order {
        class Order_Order {
            <<聚合根 · 全局关联键>>
            orderId
            userId
            status
            totalAmountCents
        }
        class OrderLineItem {
            <<实体>>
            lineItemId
            skuId
            quantity
            unitPriceCents
            itemType: PHYSICAL | SERVICE
        }
    }

    namespace Inventory {
        class SkuStock {
            <<聚合根>>
            skuId
            available
            reserved
        }
    }

    namespace Payment {
        class Payment_Payment {
            <<聚合根>>
            paymentId
            orderId
            amountCents
            status
        }
    }

    namespace Fulfillment {
        class FulfillmentOrder {
            <<聚合根>>
            fulfillmentOrderId
            orderId
            fulfillmentType: PHYSICAL | VIRTUAL
            status
        }
        class FulfillmentItem {
            <<实体>>
            fulfillmentItemId
            skuId
            quantity
        }
    }

    namespace Activity {
        class BusinessActivity {
            <<聚合根>>
            eventId
            eventType
            orderId
            occurredAt
        }
    }

    %% ── Catalog 内部组合 ──
    Category --> Category : parent
    Spu --> Category : categoryId
    Spu *-- SpecDimension
    SpecDimension *-- SpecOption
    Spu *-- Sku
    ServiceBinding --> Sku : serviceSkuId（SERVICE）
    ServiceBinding --> Spu : targetSpuId（PHYSICAL）

    %% ── User 内部 ──
    User_User *-- Address

    %% ── Cart 内部 + 引用 ──
    Cart_Cart *-- CartItem
    Cart_Cart --> User_User : userId
    CartItem --> Sku : skuId

    %% ── Order 内部 + 引用 ──
    Order_Order *-- OrderLineItem
    Order_Order --> User_User : userId
    OrderLineItem --> Sku : skuId

    %% ── 跨 BC 引用（以 orderId 为轴） ──
    SkuStock --> Sku : skuId
    Payment_Payment --> Order_Order : orderId
    FulfillmentOrder --> Order_Order : orderId
    FulfillmentOrder *-- FulfillmentItem
    FulfillmentItem --> Sku : skuId
    BusinessActivity --> Order_Order : orderId
```

## 图例

| 符号 | 含义 |
|------|------|
| `*--` | 组合（聚合内部，生命周期一致） |
| `-->` | 引用（跨聚合 / 跨 BC，通过 ID 关联） |
| `<<聚合根>>` | 聚合根，业务一致性边界 |
| `<<实体>>` | 聚合内部实体 |
| 命名空间色块 | 限界上下文（BC）边界 |

## 核心导航路径

```mermaid
flowchart LR
    subgraph 从 Order 出发
        O[Order<br/>orderId] --> U[User]
        O --> LI[OrderLineItem] --> SK[Sku] --> SP[Spu] --> CAT[Category]
        O --> PAY[Payment]
        O --> FO[FulfillmentOrder]
        O --> BA[BusinessActivity]
    end

    subgraph 从 Sku 出发
        SK2[Sku] --> SP2[Spu] --> CAT2[Category]
        SK2 --> STK[SkuStock]
        SK2 --> SB[ServiceBinding] --> SP3["Spu(PHYSICAL)"]
    end

    subgraph 从 User 出发
        U2[User] --> ADDR[Address]
        U2 --> CART[Cart] --> CI[CartItem] --> SK3[Sku]
        U2 --> O2[Order] --> MORE[...]
    end
```
